package com.muwire.gui.resultdetails

import com.muwire.core.search.UIResultEvent
import com.muwire.gui.profile.ResultPOP
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.border.Border
import javax.swing.border.TitledBorder
import java.awt.BorderLayout

import static com.muwire.gui.Translator.trans
import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class CertificateListView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    CertificateListModel model
    @Inject
    GriffonApplication application

    JPanel p
    JList<ResultPOP> senders
    JPanel detailsPanel
    
    void initUI() {
        p = builder.panel {
            borderLayout()
            panel(constraints: BorderLayout.NORTH) {
                label(text: trans("SELECT_SENDER"))
            }
            panel(constraints: BorderLayout.WEST, border: titledBorder(title: trans("SENDER"), border: etchedBorder(), titlePosition: TitledBorder.TOP)) {
                gridLayout(rows : 1, cols: 1)
                scrollPane {
                    senders = list(items: model.results)
                }
            }
            detailsPanel = panel(constraints: BorderLayout.CENTER){
                borderLayout()
            }
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        senders.setCellRenderer(new ResultListCellRenderer(application.context.get("ui-settings")))
        def selectionModel = senders.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            detailsPanel.removeAll()
            detailsPanel.updateUI()
            UIResultEvent event = senders.getSelectedValue()?.getEvent()
            if (event == null)
                return
            def mvc = model.tabGroups.get(event.sender)
            detailsPanel.add(mvc.view.p, BorderLayout.CENTER)
            detailsPanel.updateUI()
        })
    }
    
    void refresh() {
        senders.setListData(model.results.toArray(new ResultPOP[0]))
    }
}
