package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.SwingConstants

import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class FeedConfigurationView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    FeedConfigurationModel model

    def dialog
    def p
    def mainFrame
    
    def autoDownloadCheckbox
    def sequentialCheckbox
    def itemsToKeepField
    def updateIntervalField
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, "Feed Configuration", true)
        dialog.setResizable(false)
        
        p = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.NORTH) {
                label("Configuration for feed " + model.feed.getPublisher().getHumanReadableName())
            }
            panel (constraints : BorderLayout.CENTER) {
                gridBagLayout()
                label(text : "Automatically download files from feed", constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                autoDownloadCheckbox = checkBox(selected : bind {model.autoDownload}, constraints : gbc(gridx: 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                label(text : "Download files from feed sequentially", constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                sequentialCheckbox = checkBox(selected : bind {model.sequential}, constraints : gbc(gridx: 1, gridy : 1, anchor : GridBagConstraints.LINE_END))
                label(text : "Feed items to store on disk (-1 means unlimited)", constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx: 100))
                itemsToKeepField = textField(text : bind {model.itemsToKeep}, constraints:gbc(gridx :1, gridy:2, anchor : GridBagConstraints.LINE_END))
                label(text : "Feed refresh frequency in minutes", constraints : gbc(gridx: 0, gridy : 3, anchor : GridBagConstraints.LINE_START, weightx: 100))
                updateIntervalField = textField(text : bind {model.updateInterval}, constraints:gbc(gridx :1, gridy:3, anchor : GridBagConstraints.LINE_END))
            }
            panel (constraints : BorderLayout.SOUTH) {
                button(text : "Save", saveAction)
                button(text : "Cancel", cancelAction)
            } 
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        dialog.getContentPane().add(p)
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