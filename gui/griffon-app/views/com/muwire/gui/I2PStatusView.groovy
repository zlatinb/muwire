package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.SwingConstants

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class I2PStatusView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    I2PStatusModel model

    def mainFrame
    def dialog
    def panel
    def buttonsPanel

    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")

        dialog = new JDialog(mainFrame, "I2P Status", true)

        panel = builder.panel {
            gridBagLayout()
            label(text : "Network status", constraints : gbc(gridx:0, gridy:0))
            label(text : bind {model.networkStatus}, constraints : gbc(gridx: 1, gridy:0))
            label(text: "Floodfill", constraints : gbc(gridx: 0, gridy : 1))
            label(text : bind {model.floodfill}, constraints : gbc(gridx:1, gridy:1))
            label(text : "NTCP Connections", constraints : gbc(gridx:0, gridy:2))
            label(text : bind {model.ntcpConnections}, constraints : gbc(gridx: 1, gridy:2))
            label(text : "SSU Connections", constraints : gbc(gridx:0, gridy:3))
            label(text : bind {model.ssuConnections}, constraints : gbc(gridx: 1, gridy:3))
            label(text : "Participating Tunnels", constraints : gbc(gridx:0, gridy:4))
            label(text : bind {model.participatingTunnels}, constraints : gbc(gridx: 1, gridy:4))
            label(text : "Participating Bandwidth", constraints : gbc(gridx:0, gridy:5))
            label(text : bind {model.participatingBW}, constraints : gbc(gridx: 1, gridy:5))
            label(text : "Active Peers", constraints : gbc(gridx:0, gridy:6))
            label(text : bind {model.activePeers}, constraints : gbc(gridx: 1, gridy:6))
            label(text : "Receive Bps (15 seconds)", constraints : gbc(gridx:0, gridy:7))
            label(text : bind {model.receiveBps}, constraints : gbc(gridx: 1, gridy:7))
            label(text : "Send Bps (15 seconds)", constraints : gbc(gridx:0, gridy:8))
            label(text : bind {model.sendBps}, constraints : gbc(gridx: 1, gridy:8))
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
