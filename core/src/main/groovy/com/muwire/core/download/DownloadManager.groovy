package com.muwire.core.download

import com.muwire.core.connection.I2PConnector
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileHasher
import com.muwire.core.mesh.Mesh
import com.muwire.core.mesh.MeshManager
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService
import com.muwire.core.util.DataUtil

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.i2p.data.Base64
import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.UILoadedEvent

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors

public class DownloadManager {
    
    private final EventBus eventBus
    private final TrustService trustService
    private final MeshManager meshManager
    private final MuWireSettings muSettings
    private final I2PConnector connector
    private final Executor executor
    private final File incompletes, home
    private final Persona me
    
    private final Map<InfoHash, Downloader> downloaders = new ConcurrentHashMap<>()
    
    public DownloadManager(EventBus eventBus, TrustService trustService, MeshManager meshManager, MuWireSettings muSettings,
        I2PConnector connector, File home, Persona me) {
        this.eventBus = eventBus
        this.trustService = trustService
        this.meshManager = meshManager
        this.muSettings = muSettings
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
        destinations.addAll(e.sources)
        destinations.remove(me.destination)
        
        Pieces pieces = getPieces(infohash, size, pieceSize)
        
        def downloader = new Downloader(eventBus, this, me, e.target, size,
            infohash, pieceSize, connector, destinations,
            incompletes, pieces)
        downloaders.put(infohash, downloader)
        persistDownloaders()
        executor.execute({downloader.download()} as Runnable)
        eventBus.publish(new DownloadStartedEvent(downloader : downloader))
    }
    
    public void onUIDownloadCancelledEvent(UIDownloadCancelledEvent e) {
        downloaders.remove(e.downloader.infoHash)
        persistDownloaders()
    }
    
    public void onUIDownloadPausedEvent(UIDownloadPausedEvent e) {
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
            InfoHash infoHash
            if (json.hashList != null) {
                byte[] hashList = Base64.decode(json.hashList)
                infoHash = InfoHash.fromHashList(hashList)
            } else {
                byte [] root = Base64.decode(json.hashRoot)
                infoHash = new InfoHash(root)
            }
            
            Pieces pieces = getPieces(infoHash, (long)json.length, json.pieceSizePow2)
            
            def downloader = new Downloader(eventBus, this, me, file, (long)json.length,
                infoHash, json.pieceSizePow2, connector, destinations, incompletes, pieces)
            if (json.paused != null)
                downloader.paused = json.paused
            if (!downloader.paused)
                downloaders.put(infoHash, downloader)
            downloader.download()
            eventBus.publish(new DownloadStartedEvent(downloader : downloader))
        }
    }
    
    private Pieces getPieces(InfoHash infoHash, long length, int pieceSizePow2) {
        int pieceSize = 0x1 << pieceSizePow2
        int nPieces = (int)(length / pieceSize)
        if (length % pieceSize != 0)
            nPieces++
        Mesh mesh = meshManager.getOrCreate(infoHash, nPieces)
        mesh.pieces
    }
    
    void onSourceDiscoveredEvent(SourceDiscoveredEvent e) {
        Downloader downloader = downloaders.get(e.infoHash)
        if (downloader == null)
            return
        boolean ok = false
        switch(trustService.getLevel(e.source.destination)) {
            case TrustLevel.TRUSTED: ok = true; break
            case TrustLevel.NEUTRAL: ok = muSettings.allowUntrusted; break
            case TrustLevel.DISTRUSTED: ok = false; break
        }
        
        if (ok)
            downloader.addSource(e.source.destination)
    }
    
    void onFileDownloadedEvent(FileDownloadedEvent e) {
        downloaders.remove(e.downloader.infoHash)
        persistDownloaders()
    }
    
    private void persistDownloaders() {
        File downloadsFile = new File(home,"downloads.json")
        downloadsFile.withPrintWriter { writer -> 
            downloaders.values().each { downloader ->
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
                    
                    InfoHash infoHash = downloader.getInfoHash()
                    if (infoHash.hashList != null)
                        json.hashList = Base64.encode(infoHash.hashList)
                    else
                        json.hashRoot = Base64.encode(infoHash.getRoot())
                        
                    json.paused = downloader.paused
                    writer.println(JsonOutput.toJson(json))
                }
            }
        }
    }
    
    public void shutdown() {
        downloaders.values().each { it.stop() }
        Downloader.executorService.shutdownNow()
    }
}
