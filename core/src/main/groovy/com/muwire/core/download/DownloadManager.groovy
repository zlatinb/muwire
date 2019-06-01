package com.muwire.core.download

import com.muwire.core.connection.I2PConnector
import com.muwire.core.EventBus

import java.util.concurrent.Executor
import java.util.concurrent.Executors

public class DownloadManager {
    
    private final EventBus eventBus
    private final I2PConnector connector
    private final Executor executor
    private final File incompletes
    
    public DownloadManager(EventBus eventBus, I2PConnector connector, File incompletes) {
        this.eventBus = eventBus
        this.connector = connector
        this.incompletes = incompletes
        incompletes.mkdir()
        this.executor = Executors.newCachedThreadPool({ r ->
            Thread rv = new Thread(r)
            rv.setName("download-worker")
            rv.setDaemon(true)
            rv
        })
    }
    
    
    public void onUIDownloadEvent(UIDownloadEvent e) {
        def downloader = new Downloader(this, e.target, e.result.size,
            e.result.infohash, e.result.pieceSize, connector, e.result.sender.destination,
            incompletes)
        executor.execute({downloader.download()} as Runnable)
        eventBus.publish(new DownloadStartedEvent(downloader : downloader))
    }
    
    void resume(Downloader downloader) {
        executor.execute({downloader.download() as Runnable})
    }
}
