package com.muwire.gui

import java.awt.Desktop

import javax.swing.JDialog
import javax.swing.JOptionPane
import javax.swing.SwingWorker

import com.muwire.core.download.Downloader

class DownloadPreviewer extends SwingWorker {
    
    private final Downloader downloader
    private final DownloadPreviewView view

    DownloadPreviewer(Downloader downloader, DownloadPreviewView view) {
        this.downloader = downloader
        this.view = view
    }
    
    @Override
    protected Object doInBackground() throws Exception {
        downloader.generatePreview()
    }
    
    @Override
    public void done() {
        File previewFile = get()
        view.dialog.setVisible(false)
        view.mvcGroup.destroy()
        if (previewFile == null)
            JOptionPane.showMessageDialog(null, "Generating preview file failed", "Preview Failed", JOptionPane.ERROR_MESSAGE)
        else 
            Desktop.getDesktop().open(previewFile)
    }
}
