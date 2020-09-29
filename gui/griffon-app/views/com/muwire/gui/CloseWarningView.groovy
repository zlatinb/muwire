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
class CloseWarningView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    CloseWarningModel model

    def mainFrame
    def dialog
    def panel
    def checkbox
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        
        dialog = new JDialog(mainFrame, trans("CLOSE_MUWIRE_QUESTION"), true)
        panel = builder.panel {
            gridBagLayout()
            label(text : trans("MINIMIZE_OR_EXIT"), constraints : gbc(gridx: 0, gridy: 0, gridwidth : 2))
            label(text : "\n", constraints : gbc(gridx : 0, gridy : 1)) // TODO: real padding
            label(text : trans("REMEMBER_DECISION"), constraints : gbc(gridx: 0, gridy : 2, weightx: 100, anchor : GridBagConstraints.LINE_END))
            checkbox = checkBox(selected : bind {model.closeWarning}, constraints : gbc(gridx: 1, gridy :2))
            panel (constraints : gbc(gridx: 0, gridy : 3, gridwidth : 2)) {
                button(text : trans("OPTIONS_MINIMIZE_TO_TRAY"), closeAction)
                button(text : trans("EXIT_MUWIRE"), exitAction)
            }
        }
        dialog.getContentPane().add(panel)
        dialog.pack()
        dialog.setResizable(false)
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mainFrame.setVisible(false)
                mvcGroup.destroy()
            }
        })
    }
    
    void mvcGroupInit(Map<String,String> args) {
        dialog.show()
    }
}