package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.SwingConstants

import java.awt.GridBagConstraints
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class CertificateWarningView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder

    def mainFrame
    def dialog
    def panel
    def checkbox
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, trans("CERTIFICATE_WARNING"), true)
        
        panel = builder.panel {
            gridBagLayout()
            label(text : trans("CERTIFICATE_WARNING_TEXT1"), constraints :gbc(gridx: 0, gridy : 0, gridwidth : 2))
            label(text : trans("CERTIFICATE_WARNING_TEXT2"), constraints : gbc(gridx:0, gridy : 1, gridwidth: 2))
            label(text : trans("CERTIFICATE_WARNING_TEXT3"), constraints : gbc(gridx:0, gridy: 2, gridwidth:2))
            label(text : "\n", constraints : gbc(gridx:0, gridy:3)) // TODO: real padding
            label(text : "   " + trans("CERTIFICATE_UNDERSTAND"), constraints : gbc(gridx:0, gridy:4, anchor : GridBagConstraints.LINE_END))
            checkbox = checkBox(constraints : gbc(gridx:1, gridy:4, anchor : GridBagConstraints.LINE_START))
            panel (constraints : gbc(gridx :0, gridy : 5, gridwidth : 2)) {
                button(text : trans("OK"), dismissAction)
            }
        }
        
        dialog.getContentPane().add(panel)
        dialog.pack()
        dialog.setResizable(false)
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                dialog.setVisible(false)
            }
        })
    }
    
    void mvcGroupInit(Map<String,String> args) {
        dialog.show()
    }
}