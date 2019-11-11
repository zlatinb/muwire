package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.swing.SwingConstants

import java.awt.BorderLayout

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class ChatRoomView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ChatRoomModel model
    @MVCMember @Nonnull
    ChatRoomController controller

    def pane
    def parent
    
    void initUI() {
        pane = builder.panel {
            borderLayout()
            panel(constraints : BorderLayout.CENTER) {
                textArea(editable : false)
            }
            panel(constraints : BorderLayout.SOUTH) {
                borderLayout()
                textField(actionPerformed : {controller.say()}, constraints : BorderLayout.CENTER)
                button(text : "Say", constraints : BorderLayout.EAST, sayAction)
            }
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        parent = mvcGroup.parentGroup.view.builder.getVariable(model.tabName)
        parent.addTab(model.room, pane)
        
        int index = parent.indexOfComponent(pane)
        parent.setSelectedIndex(index)
        
        def tabPanel = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.CENTER) {
                label(text : model.room)
            }
            button(icon : imageIcon("/close_tab.png"), preferredSize: [20, 20], constraints : BorderLayout.EAST,
                actionPerformed : closeTab )
        }
        parent.setTabComponentAt(index, tabPanel)
    }
    
    def closeTab = {
        int index = parent.indexOfComponent(pane)
        parent.removeTabAt(index)
        mvcGroup.destroy()
    }
}