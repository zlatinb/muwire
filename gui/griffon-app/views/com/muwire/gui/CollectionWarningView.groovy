package com.muwire.gui

import griffon.core.artifact.GriffonView

import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.SwingConstants
import javax.annotation.Nonnull

import static com.muwire.gui.Translator.trans

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

@ArtifactProviderFor(GriffonView)
class CollectionWarningView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    CollectionWarningModel model

    JCheckBox rememberCheckbox
    
    def mainFrame
    def dialog
    def mainPanel
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, trans("COLLECTION_WARNING_TITLE"), true)
        dialog.setResizable(true)
        
        mainPanel = builder.panel {
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                label(text : trans("COLLECTION_WARNING_BODY1", model.fileName))
                label(text : trans("COLLECTION_WARNING_BODY2"))
            }
            scrollPane(constraints : BorderLayout.CENTER) {
                list(items : model.collections)
            }
            panel(constraints : BorderLayout.SOUTH) {
                rememberCheckbox = checkBox(selected : false)
                label(text : trans("REMEMBER_DECISION"))
                button(text : trans("UNSHARE"), unshareAction )
                button(text : trans("CANCEL"), cancelAction)
            }
        }
        
        dialog.getContentPane().add(mainPanel)
        dialog.pack()
        dialog.setResizable(false)
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
    }
    
    void mvcGroupInit(Map<String,String> args) {
        dialog.show()
    }
}