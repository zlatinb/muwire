package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.SwingConstants

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class UpdateView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    UpdateModel model

    def mainFrame
    def dialog
    def p
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        String title = model.downloaded != null ? trans("UPDATE_DOWNLOADED") : trans("UPDATE_AVAILABLE")
        dialog = new JDialog(mainFrame, title, true)
        dialog.setResizable(true)
        
        p = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.CENTER) {
                scrollPane {
                    def text = model.downloaded != null ? model.downloaded.text : model.available.text
                    textArea(text : text, rows : 20, columns : 50, editable : false, lineWrap : true, wrapStyleWord : true)
                }
            }
            panel (constraints : BorderLayout.SOUTH) {
                if (model.available != null)
                    button(text : trans("FIND"), toolTipText: trans("TOOLTIP_FIND"), searchAction)
                else if (model.downloaded != null && model.core.autoUpdater != null)
                    button(text : trans("RESTART"), toolTipText: trans("TOOLTIP_RESTART"), restartAction)
                button(text : trans("CLOSE"), closeAction)
            }
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        dialog.getContentPane().add(p)
        dialog.pack()
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.show()
    }
}