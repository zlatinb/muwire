package com.muwire.gui

import com.muwire.core.filecert.Certificate
import com.muwire.core.search.UIResultEvent
import com.muwire.gui.ResultDetailsModel.CertsModel
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.DataHelper

import javax.annotation.Nonnull
import javax.swing.JButton
import javax.swing.JLabel
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
    JTable sendersTable
    JPanel senderDetailsPanel
    JTabbedPane tabs
    
    List<CertsPanel> certsPanelList = []
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
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
                        label(text: trans("YOU_ALREADY_HAVE_FILE", model.localFiles.size()),
                            constraints: BorderLayout.NORTH)
                        scrollPane(constraints: BorderLayout.CENTER) {
                            list(items : model.localFiles.collect {it.getCachedPath()})
                        }
                    }
                }
                panel(border: etchedBorder(), constraints: gbc(gridx: 0, gridy: gridy++, weightx: 100, fill: GridBagConstraints.HORIZONTAL)) {
                    borderLayout()
                    label(text: trans("RESULTS_CAME_FROM"), constraints: BorderLayout.NORTH)
                    scrollPane(constraints: BorderLayout.CENTER) {
                        sendersTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                            tableModel(list: model.results) {
                                closureColumn(header: trans("SENDER"), preferredWidth: 200, read : {it.sender.getHumanReadableName()})
                                closureColumn(header: trans("NAME"), read : {HTMLSanitizer.sanitize(it.getFullPath())})
                            }
                        }
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
                showSelectSender.call()
                return
            }
            showTabbedPane.call()
            tabs.removeAll()
            row = sendersTable.rowSorter.convertRowIndexToModel(row)
            UIResultEvent event = model.results[row]
            
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
        private final ResultDetailsModel.CertsModel model
        private final JLabel statusLabel
        private JTable certsTable
        CertsPanel(ResultDetailsModel.CertsModel model) {
            this.model = model
            setLayout(new BorderLayout())
            statusLabel = new JLabel()
            add(statusLabel, BorderLayout.NORTH)

            JScrollPane scrollPane = builder.scrollPane {
                certsTable = builder.table {
                    tableModel(list: model.certificates) {
                        closureColumn(header: trans("NAME"), read: { Certificate c -> c.name.name })
                    }
                }
            }
            
            add(scrollPane, BorderLayout.CENTER)
        }
        
        void refresh() {
            if (model.status != null)
                statusLabel.setText(trans(model.status.name()))
            certsTable.model.fireTableDataChanged()
        }
    }
}
