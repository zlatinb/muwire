package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.swing.SwingConstants

import com.muwire.core.chat.ChatServer

import java.awt.BorderLayout

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class ChatServerView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ChatServerModel model
    @MVCMember @Nonnull
    ChatServerController controller

    def pane
    def parent

    void initUI() {
        pane = builder.panel {
            borderLayout()
            tabbedPane(id : model.host.getHumanReadableName()+"-chat-rooms", constraints : BorderLayout.CENTER)
            panel(constraints : BorderLayout.SOUTH) {
                gridLayout(rows : 1, cols : 3)
                panel {}
                panel {
                    button(text : bind {model.buttonText}, enabled : bind {model.disconnectActionEnabled}, disconnectAction)
                }
                panel {
                    label(text : "Connection Status ")
                    label(text : bind {model.status.toString()})
                }
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
        params['roomTabName'] = 'Console'
        params['console'] = true
        params['host'] = model.host
        mvcGroup.createMVCGroup("chat-room",model.host.getHumanReadableName()+"-"+ChatServer.CONSOLE, params) 
    }

    def closeTab = {
        controller.disconnect()
        int index = parent.indexOfComponent(pane)
        parent.removeTabAt(index)
        mvcGroup.destroy()
    }
}