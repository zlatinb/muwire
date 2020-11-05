package com.muwire.gui

import griffon.core.artifact.GriffonView

import static com.muwire.gui.Translator.trans

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JTextArea
import javax.swing.SwingConstants
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class AddContactView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    AddContactModel model

    def mainFrame
    def dialog
    def p
    JTextArea idArea
    JTextArea reasonArea
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, trans("ADD_CONTACT_TITLE"), true)
        dialog.setResizable(false)
        
        p = builder.panel {
            gridLayout(rows : 2, cols : 1)
            panel {
                borderLayout()
                panel(constraints : BorderLayout.NORTH) {
                    label(text : trans("ADD_CONTACT_BODY"))
                }
                scrollPane(constraints : BorderLayout.CENTER) {
                    idArea = textArea(editable : true, lineWrap : true, wrapStyleWord : true)
                }
            }
            panel {
                borderLayout()
                panel(constraints : BorderLayout.NORTH) {
                    label(text : trans("ENTER_REASON_OPTIONAL"))
                }
                scrollPane(constraints : BorderLayout.CENTER) {
                    reasonArea = textArea(editable : true, lineWrap : true, wrapStyleWord : true)
                }
                panel(constraints : BorderLayout.SOUTH) {
                    button(text : trans("ADD_CONTACT"), addAction)
                    button(text : trans("CANCEL"), cancelAction)
                }
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