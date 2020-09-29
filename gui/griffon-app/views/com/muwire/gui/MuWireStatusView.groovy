package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans
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

        dialog = new JDialog(mainFrame, trans("MUWIRE_STATUS"), true)

        panel = builder.panel {
            gridBagLayout()
            panel(border : titledBorder(title : trans("CONNECTIONS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy: 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("INCOMING"), constraints : gbc(gridx:0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.incomingConnections}, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OUTGOING"), constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.outgoingConnections}, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
            }
            panel(border : titledBorder(title : trans("HOSTS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("KNOWN"), constraints : gbc(gridx:0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.knownHosts}, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("FAILING"), constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.failingHosts}, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
                label(text : trans("HOPELESS"), constraints : gbc(gridx:0, gridy:2, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.hopelessHosts}, constraints : gbc(gridx:1, gridy:2, anchor : GridBagConstraints.LINE_END))
            }
            panel(border : titledBorder(title : trans("FILES"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 2, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("SHARED"), constraints : gbc(gridx:0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.sharedFiles}, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("DOWNLOADING"), constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.downloads}, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
                label(text : trans("TIMES_BROWSED"), constraints : gbc(gridx:0, gridy:2, anchor: GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.browsed}, constraints : gbc(gridx: 1, gridy: 2, anchor : GridBagConstraints.LINE_END))
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