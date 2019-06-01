package com.muwire.core.download

import com.muwire.core.connection.I2PConnector

import net.i2p.data.Base64

import com.muwire.core.EventBus
import com.muwire.core.Persona

import java.util.concurrent.Executor
import java.util.concurrent.Executors

public class DownloadManager {
    
    private final EventBus eventBus
    private final I2PConnector connector
    private final Executor executor
    private final File incompletes
    private final String meB64
    
    public DownloadManager(EventBus eventBus, I2PConnector connector, File incompletes, Persona me) {
        this.eventBus = eventBus
        this.connector = connector
        this.incompletes = incompletes
        
        def baos = new ByteArrayOutputStream()
        me.write(baos)
        this.meB64 = Base64.encode(baos.toByteArray())
        
        incompletes.mkdir()
        
        this.executor = Executors.newCachedThreadPool({ r ->
            Thread rv = new Thread(r)
            rv.setName("download-worker")
            rv.setDaemon(true)
            rv
        })
    }
    
    
    public void onUIDownloadEvent(UIDownloadEvent e) {
        def downloader = new Downloader(this, meB64, e.target, e.result.size,
            e.result.infohash, e.result.pieceSize, connector, e.result.sender.destination,
            incompletes)
        executor.execute({downloader.download()} as Runnable)
        eventBus.publish(new DownloadStartedEvent(downloader : downloader))
    }
    
    void resume(Downloader downloader) {
        executor.execute({downloader.download() as Runnable})
    }
}
