package com.muwire.gui

import griffon.core.artifact.GriffonView

import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JFrame
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingConstants

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class NewMessageView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    NewMessageModel model

    def window
    JTextField subjectField
    JTextArea bodyArea
    
    void initUI() {
        window = builder.frame(visible : false, locationRelativeTo : null,
            defaultCloseOperation : JFrame.DISPOSE_ON_CLOSE,
            iconImage : builder.imageIcon("/MuWire-48x48.png").image){
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                borderLayout()
                panel(constraints : BorderLayout.NORTH) {
                    label(text : trans("RECIPIENT"))
                    label(text : model.recipient.getHumanReadableName())
                }
                panel(constraints : BorderLayout.SOUTH) {
                    borderLayout()
                    label(text : trans("SUBJECT"), constraints : BorderLayout.WEST)
                    subjectField = textField(constraints : BorderLayout.CENTER)
                }
            }
            scrollPane(constraints : BorderLayout.CENTER) {
                bodyArea = textArea(editable : true, rows : 10, columns : 50)
            }
            panel(constraints : BorderLayout.SOUTH) {
                button(text : trans("SEND"), sendAction)
                button(text : trans("CANCEL"), cancelAction)
            }
        }
    }
    
    void mvcGroupInit(Map<String, String> args) {
        window.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        window.pack()
        window.setVisible(true)
    }
}