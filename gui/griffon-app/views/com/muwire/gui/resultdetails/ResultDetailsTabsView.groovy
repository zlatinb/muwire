package com.muwire.gui.resultdetails

import com.muwire.core.Persona
import com.muwire.core.search.UIResultEvent
import com.muwire.gui.HTMLSanitizer
import com.muwire.gui.PersonaCellRenderer
import com.muwire.gui.PersonaComparator
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.PersonaOrProfileCellRenderer
import com.muwire.gui.profile.PersonaOrProfileComparator
import com.muwire.gui.profile.ResultPOP
import com.muwire.gui.profile.ThumbnailIcon
import com.muwire.gui.resultdetails.ResultListCellRenderer
import griffon.core.artifact.GriffonView
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull
import javax.swing.Icon
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
    JList<ResultPOP> commentsList
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
                        closureColumn(header: trans("SENDER"), preferredWidth: 150, type: PersonaOrProfile, read : {it})
                        closureColumn(header: trans("TRUST_STATUS"), preferredWidth: 30, type:String, read : { ResultPOP row ->
                            trans(model.core.trustService.getLevel(row.getPersona().destination).name()) 
                        })
                        closureColumn(header: trans("NAME"), preferredWidth: 650,  type: String, read : { ResultPOP row -> 
                            HTMLSanitizer.sanitize(row.getEvent().getFullPath())
                        })
                        closureColumn(header: trans("COMMENTS"), preferredWidth: 20, type: Boolean, read : { ResultPOP row ->
                            row.getEvent().comment != null
                        })
                        closureColumn(header: trans("CERTIFICATES"), preferredWidth: 20, type: Integer, read : { ResultPOP row ->
                            row.getEvent().certificates
                        })
                        closureColumn(header: trans("COLLECTIONS"), preferredWidth: 20, type: Integer, read: { ResultPOP row ->
                            row.getEvent().collections.size()
                        })
                    }
                }
            }
            panel(constraints: BorderLayout.SOUTH) {
                button(text: trans("BROWSE_HOST"), toolTipText: trans("TOOLTIP_BROWSE_FILES_SENDER"), 
                        enabled : bind {model.browseActionEnabled}, browseAction)
                button(text: trans("VIEW_PROFILE"), toolTipText: trans("TOOLTIP_VIEW_PROFILE"),
                        enabled: bind {model.viewProfileActionEnabled}, viewProfileAction)
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
        sendersTable.setDefaultRenderer(PersonaOrProfile.class, new PersonaOrProfileCellRenderer())
        sendersTable.rowSorter.setComparator(0, new PersonaOrProfileComparator())
        def selectionModel = sendersTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int row = sendersTable.getSelectedRow()
            if (row < 0) {
                model.viewProfileActionEnabled = false
                model.browseActionEnabled = false
                return
            }
            row = sendersTable.rowSorter.convertRowIndexToModel(row)
            UIResultEvent event = model.results[row].getEvent()
            
            model.viewProfileActionEnabled = true
            model.browseActionEnabled = event.browse
        })
        
        
        // comments tab
        if (!model.resultsWithComments.isEmpty())
            commentsPanel.getLayout().show(commentsPanel, "yes-comments")
        commentsList.setCellRenderer(new ResultListCellRenderer())
        selectionModel = commentsList.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            UIResultEvent event = commentsList.getSelectedValue()?.getEvent()
            if (event != null)
                commentTextArea.setText(event.comment)
        })
    }
    
    void mvcGroupDestroy() {
        certificateListGroup?.destroy()
        collectionListGroup?.destroy()
    }
    
    PersonaOrProfile selectedSender() {
        int row = sendersTable.getSelectedRow()
        if (row < 0)
            return null
        row = sendersTable.rowSorter.convertRowIndexToModel(row)
        model.results[row]
    }
    
    void addResultToListGroups(ResultPOP resultPOP) {
        certificateListGroup?.model?.addResult(resultPOP)
        collectionListGroup?.model?.addResult(resultPOP)
    }
    
    void refreshAll() {
        sendersTable.model.fireTableDataChanged()
        if (!model.resultsWithComments.isEmpty()) {
            commentsPanel.getLayout().show(commentsPanel,"yes-comments")
            commentsList.setListData(model.resultsWithComments.toArray(new ResultPOP[0]))
        }
        
        buildTabs()
        p.updateUI()
    }
}
