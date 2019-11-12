package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JSplitPane
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

import com.muwire.core.Persona

import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

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
    def membersTable
    def lastMembersTableSortEvent
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        if (model.console || model.privateChat) {
            pane = builder.panel {
                borderLayout()
                panel(constraints : BorderLayout.CENTER) {
                    gridLayout(rows : 1, cols : 1)
                    scrollPane {
                        roomTextArea = textArea(editable : false, lineWrap : true, wrapStyleWord : true)
                    }
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
                    gridLayout(rows : 1, cols : 1)
                    splitPane(orientation : JSplitPane.HORIZONTAL_SPLIT, continuousLayout : true, dividerLocation : 300) {
                        panel {
                            gridLayout(rows : 1, cols : 1)
                            scrollPane {
                                membersTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                                    tableModel(list : model.members) {
                                        closureColumn(header : "Name", preferredWidth: 100, type: String, read : {it.getHumanReadableName()})
                                        closureColumn(header : "Trust Status", preferredWidth: 30, type : String, read : {String.valueOf(model.core.trustService.getLevel(it.destination))})
                                    }
                                }
                            }
                        }
                        panel {
                            gridLayout(rows : 1, cols : 1)
                            scrollPane {
                                roomTextArea = textArea(editable : false, lineWrap : true, wrapStyleWord : true)
                            }
                        }
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
        parent.addTab(model.roomTabName, pane)
        
        int index = parent.indexOfComponent(pane)
        parent.setSelectedIndex(index)
        
        def tabPanel = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.CENTER) {
                label(text : model.roomTabName)
            }
            button(icon : imageIcon("/close_tab.png"), preferredSize: [20, 20], constraints : BorderLayout.EAST,
                actionPerformed : closeTab )
        }
        if (!model.console)
            parent.setTabComponentAt(index, tabPanel)
            
        if (membersTable != null) {
            
            membersTable.rowSorter.addRowSorterListener({evt -> lastMembersTableSortEvent = evt})
            membersTable.rowSorter.setSortsOnUpdates(true)
            membersTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            
            membersTable.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            if (e.button == MouseEvent.BUTTON1 && e.clickCount > 1) {
                                int selectedRow = membersTable.getSelectedRow()
                                if (lastMembersTableSortEvent != null)
                                    selectedRow = membersTable.rowSorter.convertRowIndexToModel(selectedRow)
                                Persona p = model.members[selectedRow]
                                if (p != model.core.me && !mvcGroup.parentGroup.childrenGroups.containsKey(p.getHumanReadableName()+"-private-chat")) {
                                    def params = [:]
                                    params['core'] = model.core
                                    params['tabName'] = model.tabName
                                    params['room'] = p.getHumanReadableName()
                                    params['privateChat'] = true
                                    params['host'] = model.host
                                    params['privateTarget'] = p
                                    
                                    mvcGroup.parentGroup.createMVCGroup("chat-room", p.getHumanReadableName()+"-private-chat", params)
                                }
                            }
                        }
                    })
        }
    }
    
    def closeTab = {
        int index = parent.indexOfComponent(pane)
        parent.removeTabAt(index)
        controller.leaveRoom()
        mvcGroup.destroy()
    }
}