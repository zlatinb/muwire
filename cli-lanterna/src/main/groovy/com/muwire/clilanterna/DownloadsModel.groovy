package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.TextGUIThread
import com.googlecode.lanterna.gui2.table.TableModel

import com.muwire.core.Core
import com.muwire.core.download.DownloadStartedEvent
import com.muwire.core.download.Downloader
import com.muwire.core.files.FileDownloadedEvent

import net.i2p.data.DataHelper

class DownloadsModel {
    private final TextGUIThread guiThread
    private final Core core
    private final List<Downloader> downloaders = new ArrayList<>()
    private final TableModel model = new TableModel("Name", "Status", "Progress", "Speed", "ETA")
    
    
    private long lastRetryTime
    
    DownloadsModel(TextGUIThread guiThread, Core core) {
        this.guiThread = guiThread
        this.core = core
        
        core.eventBus.register(DownloadStartedEvent.class, this)
        Timer timer = new Timer(true)
        Runnable guiRunnable = {
            refreshModel()
            resumeDownloads()
        }
        timer.schedule({
            if (core.shutdown.get())
                return
            guiThread.invokeLater(guiRunnable)
        } as TimerTask, 1000,1000)
    }
    
    void onDownloadStartedEvent(DownloadStartedEvent e) {
        guiThread.invokeLater({
            downloaders.add(e.downloader)
            refreshModel()
        })
    }
    
    private void refreshModel() {
        int rowCount = model.getRowCount()
        rowCount.times { model.removeRow(0) }
        downloaders.each { 
            String status = it.getCurrentState().toString()
            int speedInt = it.speed()
            String speed = DataHelper.formatSize2Decimal(speedInt, false) + "B/sec"
            
            int pieces = it.nPieces
            int done = it.donePieces()
            int percent = -1
            if (pieces != 0)
                percent = (done * 100 / pieces)
            String totalSize = DataHelper.formatSize2Decimal(it.length, false) + "B"
            String progress = (String.format("%2d", percent) + "% of ${totalSize}".toString())
            
            String ETA
            if (speedInt == 0)
                ETA = "Unknown"
            else {
                long remaining = (pieces - done) * it.pieceSize / speedInt
                ETA = DataHelper.formatDuration(remaining * 1000)
            }

            model.addRow([new DownloaderWrapper(it), status, progress, speed, ETA])
        }
        
    }
    
    private void resumeDownloads() {
        int retryInterval = core.muOptions.downloadRetryInterval
        if (retryInterval == 0)
            return
        retryInterval *= 1000
        long now = System.currentTimeMillis()
        if (now - lastRetryTime > retryInterval) {
            lastRetryTime = now
            downloaders.each { 
                def state = it.getCurrentState()
                if (state == Downloader.DownloadState.FAILED || state == Downloader.DownloadState.DOWNLOADING)
                    it.resume()
            }
        }
    }
}
