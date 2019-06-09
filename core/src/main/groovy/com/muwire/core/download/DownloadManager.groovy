package com.muwire.core.download

import com.muwire.core.connection.I2PConnector
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileHasher
import com.muwire.core.util.DataUtil

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.i2p.data.Base64
import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.UILoadedEvent

import java.util.concurrent.Executor
import java.util.concurrent.Executors

public class DownloadManager {
    
    private final EventBus eventBus
    private final I2PConnector connector
    private final Executor executor
    private final File incompletes, home
    private final Persona me
    
    private final Set<Downloader> downloaders = new ConcurrentHashSet<>()
    
    public DownloadManager(EventBus eventBus, I2PConnector connector, File home, Persona me) {
        this.eventBus = eventBus
        this.connector = connector
        this.incompletes = new File(home,"incompletes")
        this.home = home
        this.me = me
        
        incompletes.mkdir()
        
        this.executor = Executors.newCachedThreadPool({ r ->
            Thread rv = new Thread(r)
            rv.setName("download-worker")
            rv.setDaemon(true)
            rv
        })
    }
    
    
    public void onUIDownloadEvent(UIDownloadEvent e) {
        
        def size = e.result[0].size
        def infohash = e.result[0].infohash
        def pieceSize = e.result[0].pieceSize
        
        Set<Destination> destinations = new HashSet<>()
        e.result.each { 
            destinations.add(it.sender.destination)
        }
        
        def downloader = new Downloader(eventBus, this, me, e.target, size,
            infohash, pieceSize, connector, destinations,
            incompletes)
        downloaders.add(downloader)
        persistDownloaders()
        executor.execute({downloader.download()} as Runnable)
        eventBus.publish(new DownloadStartedEvent(downloader : downloader))
    }
    
    public void onUIDownloadCancelledEvent(UIDownloadCancelledEvent e) {
        downloaders.remove(e.downloader)
        persistDownloaders()
    }
    
    void resume(Downloader downloader) {
        executor.execute({downloader.download() as Runnable})
    }
    
    void onUILoadedEvent(UILoadedEvent e) {
        File downloadsFile = new File(home, "downloads.json")
        if (!downloadsFile.exists())
            return
        def slurper = new JsonSlurper()
        downloadsFile.eachLine {
            def json = slurper.parseText(it)
            File file = new File(DataUtil.readi18nString(Base64.decode(json.file)))
            def destinations = new HashSet<>()
            json.destinations.each { destination -> 
                destinations.add new Destination(destination)
            }
            byte[] hashList = Base64.decode(json.hashList)
            InfoHash infoHash = InfoHash.fromHashList(hashList)
            def downloader = new Downloader(eventBus, this, me, file, (long)json.length,
                infoHash, json.pieceSizePow2, connector, destinations, incompletes)
            downloader.download()
            eventBus.publish(new DownloadStartedEvent(downloader : downloader))
        }
    }
    
    void onFileDownloadedEvent(FileDownloadedEvent e) {
        downloaders.remove(e.downloader)
        persistDownloaders()
    }
    
    private void persistDownloaders() {
        File downloadsFile = new File(home,"downloads.json")
        downloadsFile.withPrintWriter { writer -> 
            downloaders.each { downloader ->
                if (!downloader.cancelled) {
                    def json = [:]
                    json.file = Base64.encode(DataUtil.encodei18nString(downloader.file.getAbsolutePath()))
                    json.length = downloader.length
                    json.pieceSizePow2 = downloader.pieceSizePow2
                    def destinations = []
                    downloader.destinations.each {
                        destinations << it.toBase64()
                    }
                    json.destinations = destinations
                    
                    json.hashList = Base64.encode(downloader.infoHash.hashList)
                    
                    writer.println(JsonOutput.toJson(json))
                }
            }
        }
    }
}
