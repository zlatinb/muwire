package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.SwingConstants

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class SignView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder

    def mainFrame
    def dialog
    def p
    def plainTextArea
    def signedTextArea
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        
        dialog = new JDialog(mainFrame, trans("SIGN_TEXT"), true)
        
        p = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.NORTH) {
                label(trans("ENTER_TEXT_TO_SIGN"))
            }
            panel (constraints : BorderLayout.CENTER) {
                gridLayout(rows : 2, cols: 1)
                scrollPane {
                    plainTextArea = textArea(rows : 10, columns : 50, editable : true, lineWrap: true, wrapStyleWord : true)
                }
                scrollPane {
                    signedTextArea = textArea(rows : 10, columns : 50, editable : false, lineWrap : true, wrapStyleWord : true)
                }
            }
            panel (constraints : BorderLayout.SOUTH) {
                button(text : trans("SIGN"), signAction)
                button(text : trans("COPY_TO_CLIPBOARD"), copyAction)
                button(text : trans("CLOSE"), closeAction)
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