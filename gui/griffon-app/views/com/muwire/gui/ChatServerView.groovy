package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans
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
    
    ChatNotificator chatNotificator

    def pane
    def parent
    def childPane

    void initUI() {
        pane = builder.panel {
            borderLayout()
            childPane = tabbedPane(id : model.host.getHumanReadableName()+"-chat-rooms", constraints : BorderLayout.CENTER)
            panel(constraints : BorderLayout.SOUTH) {
                gridLayout(rows : 1, cols : 3)
                panel {}
                panel {
                    button(text : bind {trans(model.buttonText)}, enabled : bind {model.disconnectActionEnabled}, disconnectAction)
                }
                panel {
                    label(text : trans("CONNECTION_STATUS") + " ")
                    label(text : bind {trans(model.status.name())})
                }
            }
        }
        pane.putClientProperty("mvcId",mvcGroup.mvcId)
        pane.putClientProperty("childPane", childPane)
        childPane.addChangeListener({e -> chatNotificator.roomTabChanged(e.getSource())})
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
                    String text = model.host == model.core.me ? trans("LOCAL_SERVER") : model.host.getHumanReadableName()
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
        params['chatNotificator'] = chatNotificator
        mvcGroup.createMVCGroup("chat-room",model.host.getHumanReadableName()+"-"+ChatServer.CONSOLE, params) 
    }

    def closeTab = {
        if (model.host == model.core.me) {
            mvcGroup.parentGroup.controller.stopChatServer()
        }
        else if (model.buttonText == "DISCONNECT")
            controller.disconnect()
        int index = parent.indexOfComponent(pane)
        parent.removeTabAt(index)
        mvcGroup.destroy()
    }
}