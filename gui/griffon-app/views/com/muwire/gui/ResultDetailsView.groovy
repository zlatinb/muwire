package com.muwire.gui

import com.muwire.core.Persona
import com.muwire.core.collections.FileCollection
import com.muwire.core.filecert.Certificate
import com.muwire.core.filecert.UIImportCertificateEvent
import com.muwire.core.search.UIResultEvent
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.DataHelper

import javax.annotation.Nonnull
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.border.Border
import javax.swing.border.TitledBorder
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridBagConstraints

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class ResultDetailsView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ResultDetailsModel model
    
    
    JPanel p
    int rowHeight
    
    JTabbedPane tabs
    
    JPanel sendersPanel
    JTable sendersTable
    
    JPanel commentsPanel
    JList<UIResultEvent> commentsList
    JTextArea commentTextArea
    
    Map<Persona, CertsPanel> certsPanelMap = [:]
    Map<Persona, CollectionsPanel> collectionsPanelMap = [:]
    
    void initUI() {
        rowHeight = application.context.get("row-height")
        
        p = builder.panel {
            borderLayout()
            tabs = tabbedPane(constraints: BorderLayout.CENTER)
        }
        
        sendersPanel = builder.panel {
            borderLayout()
            panel(constraints: BorderLayout.NORTH) {
                label(text: trans("RESULTS_CAME_FROM"))
            }
            scrollPane(constraints: BorderLayout.CENTER) {
                sendersTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list: model.results) {
                        closureColumn(header: trans("SENDER"), preferredWidth: 150, type: String, read : {it.sender.getHumanReadableName()})
                        closureColumn(header: trans("TRUST_STATUS"), preferredWidth: 30, type:String, read : {
                            model.core.trustService.getLevel(it.sender.destination).name()
                        })
                        closureColumn(header: trans("NAME"), preferredWidth: 650,  type: String, read : {HTMLSanitizer.sanitize(it.getFullPath())})
                        closureColumn(header: trans("COMMENTS"), preferredWidth: 20, type: Boolean, read : {it.comment != null})
                        closureColumn(header: trans("CERTIFICATES"), preferredWidth: 20, type: Integer, read : {it.certificates})
                        closureColumn(header: trans("COLLECTIONS"), preferredWidth: 20, type: Integer, read: {it.collections.size()})
                    }
                }
            }
            panel(constraints: BorderLayout.SOUTH) {
                button(text: trans("BROWSE_HOST"), enabled : bind {model.browseActionEnabled}, browseAction)
                button(text: trans("COPY_FULL_ID"), enabled: bind {model.copyIdActionEnabled}, copyIdAction)
            }
        }
        
        commentsPanel = builder.panel {
            cardLayout()
            panel(constraints: "no-comments") {
                label(text: trans("NO_COMMENTS_FOR_FILE"))
            }
            panel(constraints: "yes-comments") {
                borderLayout()
                panel(constraints: BorderLayout.WEST, border: titledBorder(title: trans("SENDER"), border: etchedBorder(), titlePosition: TitledBorder.TOP)) {
                    borderLayout()
                    commentsList = list(items: model.resultsWithComments, constraints: BorderLayout.CENTER)
                }
                panel(constraints: BorderLayout.CENTER, border: titledBorder(title: trans("COMMENT"), border: etchedBorder(), titlePosition: TitledBorder.TOP)) {
                    borderLayout()
                    commentTextArea = textArea(editable: false, lineWrap: true, wrapStyleWord: true, constraints: BorderLayout.CENTER)
                }
            }
        }
    }
    
    private JPanel buildLocalCopies() {
        List<String> localCopies = model.getLocalCopies()
        if (localCopies == null)
            return null
        builder.panel {
            borderLayout()
            panel(constraints: BorderLayout.NORTH) {
                label(text: trans("YOU_ALREADY_HAVE_FILE", localCopies.size()))
            }
            scrollPane(constraints: BorderLayout.CENTER) {
                list(items : localCopies)
            }
        }
    }
    
    void buildTabs() {
        tabs.removeAll()
        tabs.addTab(trans("SENDERS"), sendersPanel)
        JPanel localCopies = buildLocalCopies()
        if (localCopies != null)
            tabs.addTab(trans("LOCAL_COPIES"), localCopies)
        if (!model.resultsWithComments.isEmpty())
            tabs.addTab(trans("COMMENTS"), commentsPanel)
    }
    
    void mvcGroupInit(Map<String,String> args) {
        
        // all senders table
        def selectionModel = sendersTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int row = sendersTable.getSelectedRow()
            if (row < 0) {
                model.copyIdActionEnabled = false
                model.browseActionEnabled = false
                return
            }
            row = sendersTable.rowSorter.convertRowIndexToModel(row)
            UIResultEvent event = model.results[row]
            
            model.copyIdActionEnabled = true
            model.browseActionEnabled = event.browse
        })
        
        
        // comments tab
        if (!model.resultsWithComments.isEmpty())
            commentsPanel.getLayout().show(commentsPanel, "yes-comments")
        commentsList.setCellRenderer(new ResultCellRenderer())
        selectionModel = commentsList.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            UIResultEvent event = commentsList.getSelectedValue()
            if (event != null)
                commentTextArea.setText(event.comment)
        })
    }
    
    Persona selectedSender() {
        int row = sendersTable.getSelectedRow()
        if (row < 0)
            return null
        row = sendersTable.rowSorter.convertRowIndexToModel(row)
        model.results[row].sender
    }
    
    void refreshAll() {
        sendersTable.model.fireTableDataChanged()
        if (!model.resultsWithComments.isEmpty()) {
            commentsPanel.getLayout().show(commentsPanel,"yes-comments")
            commentsList.setListData(model.resultsWithComments.toArray(new UIResultEvent[0]))
        }
        
        buildTabs()
        p.updateUI()
    }
    
    void refreshCertificates() {
        certsPanelMap[selectedSender()]?.refresh()
    }
    
    void refreshCollections() {
        collectionsPanelMap[selectedSender()]?.refresh()
    }
    
    private class CertsPanel extends JPanel {
        private final ResultDetailsModel.CertsModel certsModel
        private final JLabel statusLabel, countLabel
        private JTable certsTable
        private JButton importButton, viewCommentButton
        CertsPanel(ResultDetailsModel.CertsModel certsModel) {
            this.certsModel = certsModel
            setLayout(new BorderLayout())
            def labelPanel = new JPanel()
            statusLabel = new JLabel()
            countLabel = new JLabel()
            labelPanel.add(statusLabel)
            labelPanel.add(countLabel)
            add(labelPanel, BorderLayout.NORTH)

            JScrollPane scrollPane = builder.scrollPane {
                certsTable = builder.table(autoCreateRowSorter: true, rowHeight: rowHeight) {
                    tableModel(list: certsModel.certificates) {
                        closureColumn(header: trans("ISSUER"), preferredWidth: 150, type:String, 
                                read:{it.issuer.getHumanReadableName()})
                        closureColumn(header: trans("TRUST_STATUS"), preferredWidth: 30, type:String, 
                                read:{trans(model.core.trustService.getLevel(it.issuer.destination).name())})
                        closureColumn(header: trans("NAME"), preferredWidth: 450, 
                                read: { Certificate c -> HTMLSanitizer.sanitize(c.name.name) })
                        closureColumn(header: trans("ISSUED"), preferredWidth: 50, type: Long, 
                                read : {it.timestamp})
                        closureColumn(header: trans("COMMENTS"), preferredWidth: 20, type: Boolean, 
                                read: {it.comment != null})
                    }
                }
            }
            add(scrollPane, BorderLayout.CENTER)
            
            JPanel buttonPanel = builder.panel {
                importButton = button(text: trans("IMPORT"))
                viewCommentButton = button(text : trans("VIEW_COMMENT"))
            }
            add(buttonPanel, BorderLayout.SOUTH)
            
            certsTable.setDefaultRenderer(Long.class, new DateRenderer())
            certsTable.rowSorter.setSortsOnUpdates(true)
            def selectionModel = certsTable.getSelectionModel()
            selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            selectionModel.addListSelectionListener({
                int [] rows = certsTable.getSelectedRows()
                importButton.setEnabled(rows.length > 0)
                viewCommentButton.setEnabled(false)
                if (rows.length == 1) {
                    rows[0] = certsTable.rowSorter.convertRowIndexToModel(rows[0])
                    viewCommentButton.setEnabled(certsModel.certificates[rows[0]].comment != null)
                }
            })
            
            importButton.addActionListener({importCerts()})
            viewCommentButton.addActionListener({showComment()})
        }
        
        void refresh() {
            if (certsModel.status != null) {
                statusLabel.setText(trans(certsModel.status.name()))
                if (certsModel.count > 0)
                    countLabel.setText("${certsModel.certificates.size()}/${certsModel.count}")
            }
            certsTable.model.fireTableDataChanged()
        }
        
        private List<Certificate> selectedCertificates() {
            int[] rows = certsTable.getSelectedRows()
            if (rows.length == 0)
                return Collections.emptyList()
            for (int i = 0; i < rows.length; i++) {
                rows[i] = certsTable.rowSorter.convertRowIndexToModel(rows[i])
            }
            List<Certificate> rv = []
            for (int i : rows)
                rv << certsModel.certificates[i]
            rv
        }
        
        void showComment() {
            List<Certificate> selected = selectedCertificates()
            if (selected.size() != 1)
                return
            String comment = selected[0].comment.name
            def params = [:]
            params['text'] = comment
            params['name'] = trans("CERTIFICATE_COMMENT")
            mvcGroup.createMVCGroup("show-comment", params)
        }
        
        void importCerts() {
            List<Certificate> selected = selectedCertificates()
            selected.each {
                model.core.eventBus.publish(new UIImportCertificateEvent(certificate : it))
            }
            JOptionPane.showMessageDialog(null, trans("CERTIFICATES_IMPORTED"))
        }
    }
    
    private class CollectionsPanel extends JPanel {
        private final ResultDetailsModel.CollectionsModel collectionsModel
        private final JLabel statusLabel, countLabel
        private JTable collectionsTable
        private JButton viewDetailsButton, viewCommentButton
        
        CollectionsPanel(ResultDetailsModel.CollectionsModel collectionsModel) {
            this.collectionsModel = collectionsModel
            setLayout(new BorderLayout())
            def labelPanel = new JPanel()
            statusLabel = new JLabel()
            countLabel = new JLabel()
            labelPanel.add(statusLabel)
            labelPanel.add(countLabel)
            add(labelPanel, BorderLayout.NORTH)
            
            JScrollPane scrollPane = builder.scrollPane {
                collectionsTable = table(autoCreateRowSorter: true, rowHeight: rowHeight) {
                    tableModel(list : collectionsModel.collections) {
                        closureColumn(header: trans("NAME"), preferredWidth: 300, type : String, read : {HTMLSanitizer.sanitize(it.name)})
                        closureColumn(header: trans("AUTHOR"), preferredWidth: 200, type : String, read : {it.author.getHumanReadableName()})
                        closureColumn(header: trans("COLLECTION_TOTAL_FILES"), preferredWidth: 20, type: Integer, read : {it.numFiles()})
                        closureColumn(header: trans("COLLECTION_TOTAL_SIZE"), preferredWidth: 20, type: Long, read : {it.totalSize()})
                        closureColumn(header: trans("COMMENT"), preferredWidth: 20, type: Boolean, read: {it.comment != ""})
                        closureColumn(header: trans("CREATED"), preferredWidth: 50, type: Long, read: {it.timestamp})
                    }
                }
            }
            add(scrollPane, BorderLayout.CENTER)

            JPanel buttonPanel = builder.panel {
                viewCommentButton = button(text : trans("VIEW_COMMENT"))
                viewDetailsButton = button(text: trans("VIEW_COLLECTIONS"))
            }
            add(buttonPanel, BorderLayout.SOUTH)
            
            collectionsTable.columnModel.getColumn(3).setCellRenderer(new SizeRenderer())
            collectionsTable.columnModel.getColumn(5).setCellRenderer(new DateRenderer())
            collectionsTable.rowSorter.setSortsOnUpdates(true)
            def selectionModel = collectionsTable.getSelectionModel()
            selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            selectionModel.addListSelectionListener({
                int [] rows = collectionsTable.getSelectedRows()
                viewDetailsButton.setEnabled(rows.length > 0)
                viewCommentButton.setEnabled(false)
                if (rows.length == 1) {
                    rows[0] = collectionsTable.rowSorter.convertRowIndexToModel(rows[0])
                    viewCommentButton.setEnabled(collectionsModel.collections[rows[0]].comment != "")
                }
            })
            
            viewCommentButton.addActionListener({showComment()})
            viewDetailsButton.addActionListener({viewDetails()})
        }
        
        void refresh()  {
            if (collectionsModel.status != null) {
                statusLabel.setText(collectionsModel.status.name())
                if (collectionsModel.count > 0)
                    countLabel.setText("${collectionsModel.collections.size()}/${collectionsModel.count}")
            }
            collectionsTable.model.fireTableDataChanged()
        }
        
        private List<FileCollection> selectedCollections() {
            int [] rows = collectionsTable.getSelectedRows()
            if (rows.length == 0)
                return Collections.emptyList()
            for (int i = 0;i < rows.length; i++) {
                rows[i] = collectionsTable.rowSorter.convertRowIndexToModel(rows[i])
            }
            List<FileCollection> rv = []
            for (int row : rows)
                rv << collectionsModel.collections[row]
            rv
        }

        void showComment() {
            List<FileCollection> selected = selectedCollections() 
            if (selected.size() != 1)
                return
            String comment = selected[0].comment
            def params = [:]
            params['text'] = comment
            params['name'] = trans("COLLECTION_COMMENT")
            mvcGroup.createMVCGroup("show-comment", params)
        }
        
        void viewDetails() {
            List<FileCollection> selected = selectedCollections()
            if (selected.isEmpty())
                return
            def params = [:]
            params['fileName'] = model.fileName 
            params['eventBus'] = model.core.eventBus
            params['preFetchedCollections'] = selected
            params['host'] = selectedSender()
            mvcGroup.createMVCGroup("collection-tab", params)
        }
    }
    
    private static class ResultCellRenderer implements ListCellRenderer<UIResultEvent> {
        @Override
        Component getListCellRendererComponent(JList<? extends UIResultEvent> list, 
                                               UIResultEvent value, int index, 
                                               boolean isSelected, boolean cellHasFocus) {
            JLabel rv = new JLabel()
            rv.setText(value.sender.getHumanReadableName())
            if (!isSelected) {
                rv.setForeground(list.getForeground())
                rv.setBackground(list.getSelectionBackground())
            } else {
                rv.setForeground(list.getSelectionForeground())
                rv.setBackground(list.getSelectionBackground())
            }
            rv
        }
    }
}
