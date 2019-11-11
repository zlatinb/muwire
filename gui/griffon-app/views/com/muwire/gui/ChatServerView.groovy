package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.swing.SwingConstants

import java.awt.BorderLayout

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class ChatServerView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ChatServerModel model

    def pane
    def parent

    void initUI() {
        pane = builder.panel {
            borderLayout()
            tabbedPane(id : model.host.getHumanReadableName()+"-chat-rooms", constraints : BorderLayout.CENTER)
            panel(constraints : BorderLayout.SOUTH) {
                button(text : "Disconnect", enabled : bind {model.disconnectActionEnabled}, disconnectAction)
            }
        }
    }

    void mvcGroupInit(Map<String,String> args) {
        parent = mvcGroup.parentGroup.view.builder.getVariable("chat-tabs")
        parent.addTab(model.host.getHumanReadableName(), pane)

        int index = parent.indexOfComponent(pane)
        parent.setSelectedIndex(index)

        def tabPanel
        builder.with {
            tabPanel = panel {
                borderLayout()
                panel (constraints : BorderLayout.CENTER) {
                    String text = model.host == model.core.me ? "Local Server" : model.host.getHumanReadableName()
                    label(text : text)
                }
                button(icon : imageIcon("/close_tab.png"), preferredSize: [20, 20], constraints : BorderLayout.EAST,
                actionPerformed : closeTab )
            }
        }
        parent.setTabComponentAt(index, tabPanel)
        
        def params = [:]
        params['core'] = model.core
        params['tabName'] = model.host.getHumanReadableName() + "-chat-rooms"
        params['room'] = 'Console'
        mvcGroup.createMVCGroup("chat-room","Console", params) 
    }

    def closeTab = {
        int index = parent.indexOfComponent(pane)
        parent.removeTabAt(index)
        mvcGroup.destroy()
    }
}