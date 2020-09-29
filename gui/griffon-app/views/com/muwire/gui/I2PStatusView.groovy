package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans
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

        dialog = new JDialog(mainFrame, trans("I2P_STATUS"), true)

        panel = builder.panel {
            gridBagLayout()
            panel(border : titledBorder(title : trans("GENERAL"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx: 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("NETWORK_STATUS"), constraints : gbc(gridx:0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.networkStatus}, constraints : gbc(gridx: 1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text: trans("FLOODFILL"), constraints : gbc(gridx: 0, gridy : 1, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.floodfill}, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
                label(text : trans("ACTIVE_PEERS"), constraints : gbc(gridx:0, gridy:2, anchor : GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.activePeers}, constraints : gbc(gridx: 1, gridy:2, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OUR_COUNTRY"), constraints : gbc(gridx: 0, gridy: 3, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.myCountry}, constraints : gbc(gridx : 1, gridy: 3, anchor :  GridBagConstraints.LINE_END))
                label(text : trans("STRICT_COUNTRY"), constraints : gbc(gridx:0, gridy:4, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.strictCountry}, constraints : gbc(gridx : 1, gridy : 4, anchor : GridBagConstraints.LINE_END))
            }
            panel(border : titledBorder(title : trans("CONNECTIONS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx: 0, gridy: 1, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "NTCP", constraints : gbc(gridx:0, gridy:0, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.ntcpConnections}, constraints : gbc(gridx: 1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : "SSU", constraints : gbc(gridx:0, gridy:1, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.ssuConnections}, constraints : gbc(gridx: 1, gridy:1, anchor : GridBagConstraints.LINE_END))
            }
            panel(border : titledBorder(title : trans("PARTICIPATION"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx: 0, gridy: 2, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("TUNNELS"), constraints : gbc(gridx:0, gridy:0, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.participatingTunnels}, constraints : gbc(gridx: 1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("BANDWIDTH"), constraints : gbc(gridx:0, gridy:1, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.participatingBW}, constraints : gbc(gridx: 1, gridy:1, anchor : GridBagConstraints.LINE_END))
            }
            panel(border : titledBorder(title : trans("BANDWIDTH"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx: 0, gridy: 3, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("RECEIVE_15"), constraints : gbc(gridx:0, gridy:0, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {DataHelper.formatSize2Decimal(model.receiveBps,false)+"B"}, constraints : gbc(gridx: 1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("SEND_15"), constraints : gbc(gridx:0, gridy:1, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {DataHelper.formatSize2Decimal(model.sendBps, false)+"B"}, constraints : gbc(gridx: 1, gridy:1, anchor : GridBagConstraints.LINE_END))
            }
        }

        buttonsPanel = builder.panel {
            gridBagLayout()
            button(text : trans("REFRESH"), constraints: gbc(gridx: 0, gridy: 0), refreshAction)
            button(text : trans("CLOSE"), constraints : gbc(gridx : 1, gridy :0), closeAction)
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
