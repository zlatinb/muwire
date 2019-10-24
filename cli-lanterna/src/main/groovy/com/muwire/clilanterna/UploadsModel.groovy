package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.TextGUIThread
import com.googlecode.lanterna.gui2.table.TableModel

import com.muwire.core.Core
import com.muwire.core.upload.UploadEvent
import com.muwire.core.upload.UploadFinishedEvent
import com.muwire.core.upload.Uploader

import net.i2p.data.DataHelper

class UploadsModel {
    private final TextGUIThread guiThread
    private final Core core
    private final List<Uploader> uploaders = new ArrayList<>()
    private final TableModel model = new TableModel("Name","Progress","Downloader","Remote Pieces")
    
    UploadsModel(TextGUIThread guiThread, Core core) {
        this.guiThread = guiThread
        this.core = core
        
        core.eventBus.register(UploadEvent.class, this)
        core.eventBus.register(UploadFinishedEvent.class, this)
        
        Timer timer = new Timer(true)
        Runnable refreshModel = {refreshModel()}
        timer.schedule({
            guiThread.invokeLater(refreshModel)
        } as TimerTask, 1000, 1000)
        
    }
    
    void onUploadEvent(UploadEvent e) {
        guiThread.invokeLater({uploaders.add(e.uploader)})
    }
    
    void onUploadFinishedEvent(UploadFinishedEvent e) {
        guiThread.invokeLater({uploaders.remove(e.uploader)})
    }
    
    private void refreshModel() {
        int uploadersSize = model.getRowCount()
        uploadersSize.times { model.removeRow(0) }
        
        uploaders.each { 
            String name = it.getName()
            int percent = it.getProgress()
            String percentString = "$percent% of piece".toString()
            String downloader = it.getDownloader()
            
            int pieces = it.getTotalPieces()
            int done = it.getDonePieces()
            int percentTotal = -1
            if (pieces != 0)
                percentTotal = (done * 100) / pieces
            long size = it.getTotalSize()
            String totalSize = ""
            if (size > 0)
                totalSize = " of " + DataHelper.formatSize2Decimal(size, false) + "B"
            String remotePieces = String.format("%02d", percentTotal) + "% ${totalSize} ($done/$pieces) pcs".toString()
            
            model.addRow([name, percentString, downloader, remotePieces])
        }
    }
}
