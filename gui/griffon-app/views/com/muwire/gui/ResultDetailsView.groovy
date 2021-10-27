package com.muwire.gui

import com.muwire.core.Persona
import com.muwire.core.collections.FileCollection
import com.muwire.core.search.UIResultEvent
import com.muwire.gui.resultdetails.ResultListCellRenderer
import griffon.core.artifact.GriffonView
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.ListSelectionModel
import javax.swing.border.TitledBorder
import java.awt.BorderLayout

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

    private MVCGroup certificateListGroup
    
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
                panel(constraints: BorderLayout.NORTH) {
                    label(text: trans("SELECT_SENDER_COMMENT"))
                }
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
    
    private JPanel buildCertificates() {
        if (certificateListGroup == null) {
            String mvcId = model.fileName + "_" + Base64.encode(model.infoHash.getRoot())
            def params = [:]
            params.core = model.core
            params.results = new ArrayList<>(model.resultsWithCertificates)
            certificateListGroup = mvcGroup.createMVCGroup("certificate-list", mvcId, params)
        }
        certificateListGroup.view.p
    }
    
    private void buildTabs() {
        def selected = tabs.getSelectedComponent()
        tabs.removeAll()
        tabs.addTab(trans("SENDERS"), sendersPanel)
        JPanel localCopies = buildLocalCopies()
        if (localCopies != null)
            tabs.addTab(trans("LOCAL_COPIES"), localCopies)
        if (!model.resultsWithComments.isEmpty())
            tabs.addTab(trans("COMMENTS"), commentsPanel)
        if (!model.resultsWithCertificates.isEmpty())
            tabs.addTab(trans("CERTIFICATES"), buildCertificates())
        
        if (selected != null)
            tabs.setSelectedComponent(selected)
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
        commentsList.setCellRenderer(new ResultListCellRenderer())
        selectionModel = commentsList.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            UIResultEvent event = commentsList.getSelectedValue()
            if (event != null)
                commentTextArea.setText(event.comment)
        })
    }
    
    void mvcGroupDestroy() {
        certificateListGroup?.destroy()
    }
    
    Persona selectedSender() {
        int row = sendersTable.getSelectedRow()
        if (row < 0)
            return null
        row = sendersTable.rowSorter.convertRowIndexToModel(row)
        model.results[row].sender
    }
    
    void addResultToListGroups(UIResultEvent event) {
        certificateListGroup?.model?.addResult(event)
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
    
    void refreshCollections() {
        collectionsPanelMap[selectedSender()]?.refresh()
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
}
