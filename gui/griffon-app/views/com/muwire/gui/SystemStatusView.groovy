package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

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
        
        dialog = new JDialog(mainFrame, trans("SYSTEM_STATUS"), true)
        
        panel = builder.panel {
            gridBagLayout()
            panel(border : titledBorder(title : "Java", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy: 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("VENDOR") + "   ", constraints : gbc(gridx:0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.javaVendor}, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("VERSION"), constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {model.javaVersion}, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
            }
            panel(border : titledBorder(title : trans("MEMORY"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy: 1, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("USED"), constraints : gbc(gridx:0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {formatRam(model.usedRam)}, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("TOTAL"), constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {formatRam(model.totalRam)}, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
                label(text : trans("MAX"), constraints : gbc(gridx:0, gridy:2, anchor : GridBagConstraints.LINE_START, weightx : 100))
                label(text : bind {formatRam(model.maxRam)}, constraints : gbc(gridx:1, gridy:2, anchor : GridBagConstraints.LINE_END))
            }
            buttonsPanel = builder.panel {
                gridBagLayout()
                button(text : trans("REFRESH"), constraints: gbc(gridx: 0, gridy: 0), refreshAction)
                button(text : trans("CLOSE"), constraints : gbc(gridx : 1, gridy :0), closeAction)
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

    private static String formatRam(long ram) {
        StringBuffer sb = new StringBuffer(32)
        String bTrans = trans("BYTES_SHORT")
        SizeFormatter.format(ram,sb)
        sb.append(bTrans)
        sb.toString()
    }
}