package com.muwire.gui

import com.muwire.core.Persona
import com.muwire.core.filecert.Certificate
import com.muwire.core.filecert.UIImportCertificateEvent
import com.muwire.core.search.UIResultEvent
import com.muwire.gui.ResultDetailsModel.CertsModel
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.DataHelper

import javax.annotation.Nonnull
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.ListSelectionModel
import java.awt.BorderLayout
import java.awt.GridBagConstraints

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class ResultDetailsView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ResultDetailsModel model
    
    
    def parent
    def p
    int rowHeight
    JTable sendersTable
    JPanel senderDetailsPanel
    JTabbedPane tabs
    
    List<CertsPanel> certsPanelList = []
    
    void initUI() {
        rowHeight = application.context.get("row-height")
        p = builder.panel {
            borderLayout()
            panel(constraints: BorderLayout.NORTH) {
                label(text : HTMLSanitizer.sanitize(model.fileName))
                label(text : DataHelper.formatSize2(model.results[0].size, false) + trans("BYTES_SHORT"))
            }
            panel(constraints: BorderLayout.CENTER) {
                gridBagLayout()
                int gridy = 0
                if (!model.localFiles.isEmpty()) {
                    panel (border: etchedBorder(), constraints: gbc(gridx: 0, gridy: gridy++, weightx: 100, fill: GridBagConstraints.HORIZONTAL)) {
                        borderLayout()
                        panel(constraints: BorderLayout.NORTH) {
                            label(text: trans("YOU_ALREADY_HAVE_FILE", model.localFiles.size()))
                        }
                        scrollPane(constraints: BorderLayout.CENTER) {
                            list(items : model.localFiles.collect {it.getCachedPath()})
                        }
                    }
                }
                panel(border: etchedBorder(), constraints: gbc(gridx: 0, gridy: gridy++, weightx: 100, fill: GridBagConstraints.HORIZONTAL)) {
                    borderLayout()
                    panel(constraints: BorderLayout.NORTH) {
                        label(text: trans("RESULTS_CAME_FROM"))
                    }
                    scrollPane(constraints: BorderLayout.CENTER) {
                        sendersTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                            tableModel(list: model.results) {
                                closureColumn(header: trans("SENDER"), preferredWidth: 150, type: String, read : {it.sender.getHumanReadableName()})
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
                senderDetailsPanel = panel(constraints: gbc(gridx:0, gridy: gridy++, weightx: 100, weighty: 100, fill: GridBagConstraints.BOTH)) {
                    cardLayout()
                    panel(constraints: "select-sender"){
                        label(text: trans("SELECT_SENDER"))
                    }
                    tabs = tabbedPane(constraints: "sender-details")
                }
            }
        }
        
    }
    
    void mvcGroupInit(Map<String,String> args) {
        def mainFrameGroup = application.mvcGroupManager.findGroup("MainFrame")
        mainFrameGroup.model.resultDetails.add(model.key)
        parent = mainFrameGroup.view.builder.getVariable("result-tabs")
        parent.addTab(model.key, p)
        
        int index = parent.indexOfComponent(p)
        parent.setSelectedIndex(index)
        
        def tabPanel = builder.panel {
            borderLayout()
            panel(constraints : BorderLayout.CENTER) {
                label(text : model.fileName)
            }
            button(icon: imageIcon("/close_tab.png"), preferredSize: [20,20], constraints: BorderLayout.EAST,
                    actionPerformed: closeTab)
        }
        
        parent.setTabComponentAt(index, tabPanel)
        mainFrameGroup.view.showSearchWindow.call()
        
        def selectionModel = sendersTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int row = sendersTable.getSelectedRow()
            if (row < 0) {
                model.copyIdActionEnabled = false
                model.browseActionEnabled = false
                showSelectSender.call()
                return
            }
            showTabbedPane.call()
            tabs.removeAll()
            row = sendersTable.rowSorter.convertRowIndexToModel(row)
            UIResultEvent event = model.results[row]
            
            model.copyIdActionEnabled = true
            model.browseActionEnabled = event.browse
            
            if (event.comment != null) {
                def commentPanel = builder.panel {
                    borderLayout()
                    scrollPane(constraints: BorderLayout.CENTER) {
                        textArea(text: event.comment, editable: false, lineWrap: true, wrapStyleWord : true)
                    }
                }
                tabs.addTab(trans("COMMENT"),commentPanel)
            }
            if (event.certificates > 0) {
                def certsPanel
                if (model.certificates.containsKey(event.sender)) {
                    certsPanel = new CertsPanel(model.certificates[event.sender])
                    certsPanel.setPreferredSize(tabs.getPreferredSize())
                    certsPanel.refresh()
                } else {
                    certsPanel = builder.panel {
                        cardLayout()
                        panel(constraints: "fetch-certificates") {
                            label(text: trans("SENDER_HAS_CERTIFICATES", event.certificates))
                            JButton fetchButton = button(text : trans("VIEW_CERTIFICATES"))
                            fetchButton.addActionListener( {
                                def certsModel = model.registerForCertificates(event.sender)
                                def newCertsPanel = new CertsPanel(certsModel)
                                certsPanelList << newCertsPanel
                                newCertsPanel.setPreferredSize(certsPanel.getPreferredSize())
                                certsPanel.add(newCertsPanel, "view-certificates")
                                certsPanel.getLayout().last(certsPanel)
                                newCertsPanel.refresh()
                            })
                        }
                    }
                }
                tabs.addTab(trans("CERTIFICATES"), certsPanel)
            }
            if (!event.collections.isEmpty()) {
                def collectionsPanel = builder.panel {
                    label(text: "TODO show collections")
                }
                tabs.addTab(trans("COLLECTIONS"), collectionsPanel)
            }
        })
    }
    
    def closeTab = {
        def mainFrameGroup = application.mvcGroupManager.findGroup("MainFrame")
        mainFrameGroup.model.resultDetails.remove(model.key)
        
        int index = parent.indexOfTab(model.key)
        parent.removeTabAt(index)
        mvcGroup.destroy()
    }

    Persona selectedSender() {
        int row = sendersTable.getSelectedRow()
        if (row < 0)
            return null
        row = sendersTable.rowSorter.convertRowIndexToModel(row)
        model.results[row].sender
    }
    
    def showTabbedPane = {
        senderDetailsPanel.getLayout().show(senderDetailsPanel, "sender-details")
    }
    
    def showSelectSender = {
        senderDetailsPanel.getLayout().show(senderDetailsPanel, "select-sender")
    }
    
    void refreshCertificates() {
        certsPanelList.each {it.refresh()}
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
}
