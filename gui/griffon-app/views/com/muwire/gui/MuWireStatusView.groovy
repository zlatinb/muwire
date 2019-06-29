package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.SwingConstants

import com.muwire.core.Core

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class MuWireStatusView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    MuWireStatusModel model

    def mainFrame
    def dialog
    def panel
    def buttonsPanel
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        
        dialog = new JDialog(mainFrame, "MuWire Status", true)
        
        panel = builder.panel {
            gridBagLayout()
            label(text : "Incoming connections", constraints : gbc(gridx:0, gridy:0))
            label(text : bind {model.incomingConnections}, constraints : gbc(gridx:1, gridy:0))
            label(text : "Outgoing connections", constraints : gbc(gridx:0, gridy:1))
            label(text : bind {model.outgoingConnections}, constraints : gbc(gridx:1, gridy:1))
            label(text : "Known hosts", constraints : gbc(gridx:0, gridy:2))
            label(text : bind {model.knownHosts}, constraints : gbc(gridx:1, gridy:2))
            label(text : "Shared files", constraints : gbc(gridx:0, gridy:3))
            label(text : bind {model.sharedFiles}, constraints : gbc(gridx:1, gridy:3))
            label(text : "Downloads", constraints : gbc(gridx:0, gridy:4))
            label(text : bind {model.downloads}, constraints : gbc(gridx:1, gridy:4))
        }
        buttonsPanel = builder.panel {
            gridBagLayout()
            button(text : "Refresh", constraints: gbc(gridx: 0, gridy: 0), refreshAction)
            button(text : "Close", constraints : gbc(gridx : 1, gridy :0), closeAction)
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        JPanel statusPanel = new JPanel()
        statusPanel.setLayout(new BorderLayout())
        statusPanel.add(panel, BorderLayout.CENTER)
        statusPanel.add(buttonsPanel, BorderLayout.SOUTH)
        
        dialog.getContentPane().add(statusPanel)
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