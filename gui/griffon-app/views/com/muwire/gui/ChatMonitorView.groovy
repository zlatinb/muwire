package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JFrame
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

    def window
    def roomsTable
    
    void initUI() {
        int rowHeight = application.context.getAsInt("row-height")
        
        window = builder.frame (visible : false, locationRelativeTo : null,
            defaultCloseOperation : JFrame.DISPOSE_ON_CLOSE,
            iconImage : builder.imageIcon("/MuWire-48x48.png").image){
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                label(trans("CHAT_ROOMS_WITH_MESSAGES"))
            }
            scrollPane(constraints : BorderLayout.CENTER) {
                roomsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.rooms) {
                        closureColumn(header : trans("SERVER"), type: String, read : {it.server})
                        closureColumn(header : trans("ROOM"), type : String, read : {it.room})
                        closureColumn(header : trans("MESSAGES"), type : Integer, read : {it.count})
                    }
                }
            }
            panel(constraints : BorderLayout.SOUTH) {
                button(text : trans("CLOSE"), closeAction)
            }
        }
    }
    
    void updateView() {
        roomsTable.model.fireTableDataChanged()
    }
    
    void mvcGroupInit(Map<String,String> args) {
        window.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        window.pack()
        window.setVisible(true)
    }
}