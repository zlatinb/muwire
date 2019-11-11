package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JSplitPane
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
    def sayField
    def roomTextArea
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        if (model.console) {
            pane = builder.panel {
                borderLayout()
                panel(constraints : BorderLayout.CENTER) {
                    gridLayout(rows : 1, cols : 1)
                    roomTextArea = textArea(editable : false, lineWrap : true, wrapStyleWord : true)
                }
                panel(constraints : BorderLayout.SOUTH) {
                    borderLayout()
                    sayField = textField(actionPerformed : {controller.say()}, constraints : BorderLayout.CENTER)
                    button(text : "Say", constraints : BorderLayout.EAST, sayAction)
                }
            }
        } else {
            pane = builder.panel {
                borderLayout()
                panel(constraints : BorderLayout.CENTER) {
                    splitPane(orientation : JSplitPane.HORIZONTAL_SPLIT, continuousLayout : true, dividerLocation : 200)
                    panel {
                        table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                            tableModel(list : model.members) {
                                closureColumn(header : "Name", type: String, read : {it.getHumanReadableName()})
                                closureColumn(header : "Trust Status", type : String, read : {String.valueOf(model.core.trustService.getLevel(it.destination))})
                            }
                        }
                    }
                    panel {
                        gridLayout(rows : 1, cols : 1)
                        roomTextArea = textArea(editable : false, lineWrap : true, wrapStyleWord : true)
                    }
                }
                panel(constraints : BorderLayout.SOUTH) {
                    borderLayout()
                    sayField = textField(actionPerformed : {controller.say()}, constraints : BorderLayout.CENTER)
                    button(text : "Say", constraints : BorderLayout.EAST, sayAction)
                }

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
        if (!model.console)
            parent.setTabComponentAt(index, tabPanel)
    }
    
    def closeTab = {
        int index = parent.indexOfComponent(pane)
        parent.removeTabAt(index)
        mvcGroup.destroy()
    }
}