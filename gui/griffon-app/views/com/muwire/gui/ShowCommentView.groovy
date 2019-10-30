package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.swing.JDialog
import javax.swing.SwingConstants

import com.muwire.core.util.DataUtil

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class ShowCommentView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ShowCommentModel model

    def mainFrame
    def dialog
    def p
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, model.result.name, true)
        dialog.setResizable(true)
        
        
        p = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.CENTER) {
                scrollPane {
                    textArea(text : model.result.comment, rows : 20, columns : 100, editable : false, lineWrap : true, wrapStyleWord : true)
                }
            }
            panel (constraints : BorderLayout.SOUTH) {
                button(text : "Dismiss", dismissAction)
            }
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        dialog.getContentPane().add(p)
        dialog.pack()
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.addWindowListener( new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        dialog.show()
    }
}