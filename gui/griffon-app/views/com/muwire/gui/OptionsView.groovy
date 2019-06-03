package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.SwingConstants

import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class OptionsView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    OptionsModel model

    def d
    def p
    def retryField
    def updateField
    def mainFrame
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        d = new JDialog(mainFrame, "Options", true)
        d.setResizable(false)
        p = builder.panel {
            gridBagLayout()
            label(text : "Retry failed downloads every", constraints : gbc(gridx: 0, gridy: 0))
            retryField = textField(text : bind { model.downloadRetryInterval }, columns : 2, constraints : gbc(gridx: 1, gridy: 0))
            label(text : "minutes", constraints : gbc(gridx : 2, gridy: 0))
            
            label(text : "Check for updates every", constraints : gbc(gridx : 0, gridy: 1))
            updateField = textField(text : bind {model.updateCheckInterval }, columns : 2, constraints : gbc(gridx : 1, gridy: 1))
            label(text : "hours", constraints : gbc(gridx: 2, gridy : 1))
            
            button(text : "Save", constraints : gbc(gridx : 1, gridy: 2), saveAction)
            button(text : "Cancel", constraints : gbc(gridx : 2, gridy: 2), cancelAction)
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        d.getContentPane().add(p)
        d.pack()
        d.setLocationRelativeTo(mainFrame)
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        d.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        d.show()
    }
}