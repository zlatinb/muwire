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
    private CliSettings props
    private final List<UploaderWrapper> uploaders = new ArrayList<>()
    private final TableModel model = new TableModel("Name","Progress","Downloader","Remote Pieces", "Speed")
    
    UploadsModel(TextGUIThread guiThread, Core core, CliSettings props) {
        this.guiThread = guiThread
        this.core = core
        this.props = props
        
        core.eventBus.register(UploadEvent.class, this)
        core.eventBus.register(UploadFinishedEvent.class, this)
        
        Timer timer = new Timer(true)
        Runnable refreshModel = {refreshModel()}
        timer.schedule({
            guiThread.invokeLater(refreshModel)
        } as TimerTask, 1000, 1000)
        
    }
    
    void onUploadEvent(UploadEvent e) {
        guiThread.invokeLater {
            UploaderWrapper found = null
            uploaders.each { 
                if (it.uploader == e.uploader) {
                    found = it
                    return
                }
            }
            if (found != null) {
                found.uploader = e.uploader
                found.finished = false
            } else
                uploaders << new UploaderWrapper(uploader : e.uploader)
        }
    }
    
    void onUploadFinishedEvent(UploadFinishedEvent e) {
        guiThread.invokeLater {
            uploaders.each { 
                if (it.uploader == e.uploader) {
                    it.finished = true
                    return
                }
            }
        }
    }
    
    private void refreshModel() {
        int uploadersSize = model.getRowCount()
        uploadersSize.times { model.removeRow(0) }

        if (props.clearUploads) {
            uploaders.removeAll { it.finished }
        }
                
        uploaders.each { 
            String name = it.uploader.getName()
            int percent = it.uploader.getProgress()
            String percentString = "$percent% of piece".toString()
            String downloader = it.uploader.getDownloader()
            
            int pieces = it.uploader.getTotalPieces()
            int done = it.uploader.getDonePieces()
            int percentTotal = -1
            if (pieces != 0)
                percentTotal = (done * 100) / pieces
            long size = it.uploader.getTotalSize()
            String totalSize = ""
            if (size > 0)
                totalSize = " of " + DataHelper.formatSize2Decimal(size, false) + "B"
            String remotePieces = String.format("%02d", percentTotal) + "% ${totalSize} ($done/$pieces) pcs".toString()
            
            String speed = DataHelper.formatSize2Decimal(it.uploader.speed(), false) + "B/sec"
            
            
            model.addRow([name, percentString, downloader, remotePieces, speed])
        }
    }
    
    private static class UploaderWrapper {
        Uploader uploader
        boolean finished
        
        @Override
        public String toString() {
            uploader.getName()
        }
    }
}
