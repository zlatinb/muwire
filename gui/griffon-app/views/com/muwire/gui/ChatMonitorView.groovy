package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.SwingConstants

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class ChatMonitorView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ChatMonitorModel model

    def mainFrame
    def dialog
    def panel
    def roomsTable
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.getAsInt("row-height")
        dialog = new JDialog(mainFrame, "Chat Monitor", false)
        dialog.setResizable(true)
        
        panel = builder.panel {
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                label("Chat rooms with unread messages")
            }
            scrollPane(constraints : BorderLayout.CENTER) {
                roomsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.rooms) {
                        closureColumn(header : "Server", type: String, read : {it.server})
                        closureColumn(header : "Room", type : String, read : {it.room})
                        closureColumn(header : "Messages", type : Integer, read : {it.count})
                    }
                }
            }
        }
    }
    
    void updateView() {
        roomsTable.model.fireTableDataChanged()
    }
    
    void mvcGroupInit(Map<String,String> args) {
        dialog.getContentPane().add(panel)
        dialog.pack()
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        dialog.show()
    }
}