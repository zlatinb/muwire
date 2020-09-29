package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans
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
        dialog = new JDialog(mainFrame, trans("FEED_CONFIGURATION"), true)
        dialog.setResizable(false)
        
        p = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.NORTH) {
                label(trans("FEED_CONFIGURATION_FOR",model.feed.getPublisher().getHumanReadableName()))
            }
            panel (constraints : BorderLayout.CENTER) {
                gridBagLayout()
                label(text : trans("OPTIONS_FEED_AUTO_DOWNLOAD"), constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                autoDownloadCheckbox = checkBox(selected : bind {model.autoDownload}, constraints : gbc(gridx: 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_FEED_DOWNLOAD_SEQUENTIALLY"), constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                sequentialCheckbox = checkBox(selected : bind {model.sequential}, constraints : gbc(gridx: 1, gridy : 1, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_FEED_ITEMS_ON_DISK"), constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx: 100))
                itemsToKeepField = textField(text : bind {model.itemsToKeep}, constraints:gbc(gridx :1, gridy:2, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_FEED_REFRESH_FREQUENCY"), constraints : gbc(gridx: 0, gridy : 3, anchor : GridBagConstraints.LINE_START, weightx: 100))
                updateIntervalField = textField(text : bind {model.updateInterval}, constraints:gbc(gridx :1, gridy:3, anchor : GridBagConstraints.LINE_END))
            }
            panel (constraints : BorderLayout.SOUTH) {
                button(text : trans("SAVE"), saveAction)
                button(text : trans("CANCEL"), cancelAction)
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