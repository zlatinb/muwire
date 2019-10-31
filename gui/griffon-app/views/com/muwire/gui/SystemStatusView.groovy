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
class SystemStatusView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    SystemStatusModel model

    def mainFrame
    def dialog
    def panel
    def buttonsPanel
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        
        dialog = new JDialog(mainFrame, "System Status", true)
        
        panel = builder.panel {
            gridBagLayout()
            panel(border : titledBorder(title : "Java", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy: 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "Vendor   ", constraints : gbc(gridx:0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.javaVendor}, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : "Version", constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.javaVersion}, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
            }
            panel(border : titledBorder(title : "Memory", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy: 1, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "Used", constraints : gbc(gridx:0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {DataHelper.formatSize2Decimal(model.usedRam,false)+"B"}, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : "Total", constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {DataHelper.formatSize2Decimal(model.totalRam,false)+"B"}, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
                label(text : "Max", constraints : gbc(gridx:0, gridy:2, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {DataHelper.formatSize2Decimal(model.maxRam,false)+"B"}, constraints : gbc(gridx:1, gridy:2, anchor : GridBagConstraints.LINE_END))
            }
            buttonsPanel = builder.panel {
                gridBagLayout()
                button(text : "Refresh", constraints: gbc(gridx: 0, gridy: 0), refreshAction)
                button(text : "Close", constraints : gbc(gridx : 1, gridy :0), closeAction)
            }
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        JPanel statusPanel = new JPanel()
        statusPanel.setLayout(new BorderLayout())
        statusPanel.add(panel, BorderLayout.CENTER)
        statusPanel.add(buttonsPanel, BorderLayout.SOUTH)

        dialog.getContentPane().add(statusPanel)
        dialog.pack()
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