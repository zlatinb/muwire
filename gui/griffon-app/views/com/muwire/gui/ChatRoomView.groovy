package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.SpringLayout.Constraints

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
                    label(text : "Say something here: ", constraints : BorderLayout.WEST)
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
                    label(text : "Say something here: ", constraints : BorderLayout.WEST)
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
                                controller.privateMessage()
                            } else if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                                showPopupMenu(e)
                        }
                        
                        public void mouseReleased(MouseEvent e) {
                            if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                                showPopupMenu(e)
                        }
                    })
        }
    }
    
    private void showPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu()
        JMenuItem privateChat = new JMenuItem("Start Private Chat")
        privateChat.addActionListener({controller.privateMessage()})
        menu.add(privateChat)
        JMenuItem markTrusted = new JMenuItem("Mark Trusted")
        markTrusted.addActionListener({controller.markTrusted()})
        menu.add(markTrusted)
        JMenuItem markNeutral = new JMenuItem("Mark Neutral")
        markNeutral.addActionListener({controller.markNeutral()})
        menu.add(markNeutral)
        JMenuItem markDistrusted = new JMenuItem("Mark Distrusted")
        markDistrusted.addActionListener({controller.markDistrusted()})
        menu.add(markDistrusted)
        menu.show(e.getComponent(), e.getX(), e.getY())
    }
    
    Persona getSelectedPersona() {
        int selectedRow = membersTable.getSelectedRow()
        if (selectedRow < 0)
            return null
        if (lastMembersTableSortEvent != null)
            selectedRow = membersTable.rowSorter.convertRowIndexToModel(selectedRow)
        model.members[selectedRow]
    }
    
    void refreshMembersTable() {
        int selectedRow = membersTable.getSelectedRow()
        membersTable.model.fireTableDataChanged()
        membersTable.selectionModel.setSelectionInterval(selectedRow, selectedRow)
    }
    
    def closeTab = {
        int index = parent.indexOfComponent(pane)
        parent.removeTabAt(index)
        controller.leaveRoom()
        mvcGroup.destroy()
    }
}