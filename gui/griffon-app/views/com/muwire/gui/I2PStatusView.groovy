package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.DataHelper

import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.TitledBorder

import java.awt.BorderLayout
import java.awt.GridBagConstraints
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
            panel(border : titledBorder(title : "General", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx: 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "Network status", constraints : gbc(gridx:0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.networkStatus}, constraints : gbc(gridx: 1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text: "Floodfill", constraints : gbc(gridx: 0, gridy : 1, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.floodfill}, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
                label(text : "Active Peers", constraints : gbc(gridx:0, gridy:2, anchor : GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.activePeers}, constraints : gbc(gridx: 1, gridy:2, anchor : GridBagConstraints.LINE_END))
            }
            panel(border : titledBorder(title : "Connections", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx: 0, gridy: 1, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "NTCP", constraints : gbc(gridx:0, gridy:0, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.ntcpConnections}, constraints : gbc(gridx: 1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : "SSU", constraints : gbc(gridx:0, gridy:1, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.ssuConnections}, constraints : gbc(gridx: 1, gridy:1, anchor : GridBagConstraints.LINE_END))
            }
            panel(border : titledBorder(title : "Participation", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx: 0, gridy: 2, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "Tunnels", constraints : gbc(gridx:0, gridy:0, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.participatingTunnels}, constraints : gbc(gridx: 1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : "Bandwidth", constraints : gbc(gridx:0, gridy:1, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.participatingBW}, constraints : gbc(gridx: 1, gridy:1, anchor : GridBagConstraints.LINE_END))
            }
            panel(border : titledBorder(title : "Bandwidth", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx: 0, gridy: 3, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "Receive (15 seconds)", constraints : gbc(gridx:0, gridy:0, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {DataHelper.formatSize2Decimal(model.receiveBps,false)+"B"}, constraints : gbc(gridx: 1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : "Send (15 seconds)", constraints : gbc(gridx:0, gridy:1, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {DataHelper.formatSize2Decimal(model.sendBps, false)+"B"}, constraints : gbc(gridx: 1, gridy:1, anchor : GridBagConstraints.LINE_END))
            }
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
