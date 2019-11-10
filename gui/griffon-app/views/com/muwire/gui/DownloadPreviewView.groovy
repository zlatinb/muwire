package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.Box
import javax.swing.JDialog
import javax.swing.JOptionPane
import javax.swing.SwingConstants

import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class DownloadPreviewView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    DownloadPreviewModel model

    def mainFrame
    def dialog
    def panel
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        
        dialog = new JDialog(mainFrame, "Generating Preview", true)
        
        panel = builder.panel {
            vbox {
                label(text : "Generating preview for "+model.downloader.file.getName())
                Box.createVerticalGlue()
                progressBar(indeterminate : true)
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
    
    void mvcGroupInit(Map<String, String> args) {
        if (!model.downloader.isSequential())
            JOptionPane.showMessageDialog(mainFrame, "This download is not sequential, there may not be much to preview")
        DownloadPreviewer previewer = new DownloadPreviewer(model.downloader, this)
        previewer.execute()
        dialog.show()
    }
}