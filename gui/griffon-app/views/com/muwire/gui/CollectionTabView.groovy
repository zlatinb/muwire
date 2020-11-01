package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.swing.SwingConstants

import java.awt.BorderLayout

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class CollectionTabView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    CollectionTabModel model

    def parent
    def p
    
    void initUI() {
        p = builder.panel {}
    }
    
    void mvcGroupInit(Map<String, String> args) {
        def mainFrameGroup = application.mvcGroupManager.findGroup("MainFrame")
        mainFrameGroup.model.collections.add(model.uuid.toString())
        parent = mainFrameGroup.view.builder.getVariable("result-tabs")
        parent.addTab(model.uuid.toString(), p)
        
        int index = parent.indexOfComponent(p)
        parent.setSelectedIndex(index)
     
        def tabPanel
        builder.with {
            tabPanel = panel {
                borderLayout()
                panel {
                    label(text : model.fileName, constraints : BorderLayout.CENTER)
                }
                button(icon : imageIcon("/close_tab.png"), preferredSize : [20,20], constraints : BorderLayout.EAST, // TODO: in osx is probably WEST
                    actionPerformed : closeTab )
            }
        }

        parent.setTabComponentAt(index, tabPanel)
    }
    
    def closeTab = {
        def mainFrameGroup = application.mvcGroupManager.findGroup("MainFrame")
        mainFrameGroup.model.collections.remove(model.uuid.toString())
        
        int index = parent.indexOfTab(model.uuid.toString())
        parent.removeTabAt(index)
        mvcGroup.destroy()
    }
}