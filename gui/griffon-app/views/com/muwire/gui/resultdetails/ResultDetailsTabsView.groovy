package com.muwire.gui.resultdetails

import com.muwire.core.Persona
import com.muwire.core.search.UIResultEvent
import com.muwire.gui.HTMLSanitizer
import com.muwire.gui.resultdetails.ResultListCellRenderer
import griffon.core.artifact.GriffonView
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.ListSelectionModel
import javax.swing.border.TitledBorder
import java.awt.BorderLayout

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class ResultDetailsTabsView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ResultDetailsTabsModel model
    
    
    JPanel p
    int rowHeight
    
    JTabbedPane tabs
    
    JPanel sendersPanel
    JTable sendersTable
    
    JPanel commentsPanel
    JList<UIResultEvent> commentsList
    JTextArea commentTextArea

    private MVCGroup certificateListGroup
    private MVCGroup collectionListGroup
    
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
                            trans(model.core.trustService.getLevel(it.sender.destination).name())
                        })
                        closureColumn(header: trans("NAME"), preferredWidth: 650,  type: String, read : { HTMLSanitizer.sanitize(it.getFullPath())})
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
                    scrollPane (constraints: BorderLayout.CENTER) {
                        commentsList = list(items: model.resultsWithComments)
                    }
                }
                panel(constraints: BorderLayout.CENTER, border: titledBorder(title: trans("COMMENT"), border: etchedBorder(), titlePosition: TitledBorder.TOP)) {
                    borderLayout()
                    scrollPane (constraints: BorderLayout.CENTER){
                        commentTextArea = textArea(editable: false, lineWrap: true, wrapStyleWord: true)
                    }
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
            String mvcId = "certs_" + model.uuid + "_" + model.fileName + "_" + Base64.encode(model.infoHash.getRoot())
            def params = [:]
            params.core = model.core
            params.results = new ArrayList<>(model.resultsWithCertificates)
            params.uuid = model.uuid
            certificateListGroup = mvcGroup.createMVCGroup("certificate-list", mvcId, params)
        }
        certificateListGroup.view.p
    }
    
    private JPanel buildCollections() {
        if (collectionListGroup == null) {
            String mvcId = "collections_" + model.uuid + "_" + model.fileName + "_" + Base64.encode(model.infoHash.getRoot())
            def params = [:]
            params.core = model.core
            params.results = new ArrayList<>(model.resultsWithCollections)
            params.uuid = model.uuid
            collectionListGroup = mvcGroup.createMVCGroup("collection-list", mvcId, params)
        }
        collectionListGroup.view.p
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
        if (!model.resultsWithCollections.isEmpty())
            tabs.addTab(trans("COLLECTIONS"), buildCollections())
        
        if (selected != null && tabs)
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
        collectionListGroup?.destroy()
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
        collectionListGroup?.model?.addResult(event)
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
}
