package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.TitledBorder

import com.muwire.core.Core

import java.awt.BorderLayout
import java.awt.GridBagConstraints
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
            panel(border : titledBorder(title : "Connections", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy: 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "Incoming", constraints : gbc(gridx:0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.incomingConnections}, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : "Outgoing", constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.outgoingConnections}, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
            }
            panel(border : titledBorder(title : "Hosts", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "Known", constraints : gbc(gridx:0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.knownHosts}, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : "Failing", constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.failingHosts}, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
                label(text : "Hopeless", constraints : gbc(gridx:0, gridy:2, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.hopelessHosts}, constraints : gbc(gridx:1, gridy:2, anchor : GridBagConstraints.LINE_END))
            }
            panel(border : titledBorder(title : "Files", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 2, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "Shared", constraints : gbc(gridx:0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.sharedFiles}, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : "Downloading", constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.downloads}, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
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
        dialog.setSize(200,300)
        dialog.setResizable(false)
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