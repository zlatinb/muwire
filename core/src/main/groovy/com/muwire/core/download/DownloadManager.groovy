package com.muwire.core.download

import com.muwire.core.connection.I2PConnector
import com.muwire.core.filefeeds.UIDownloadFeedItemEvent
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileHasher
import com.muwire.core.files.FileManager
import com.muwire.core.mesh.Mesh
import com.muwire.core.mesh.MeshManager
import com.muwire.core.messenger.UIDownloadAttachmentEvent
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService
import com.muwire.core.util.DataUtil

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Base64
import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.UILoadedEvent
import com.muwire.core.chat.ChatManager
import com.muwire.core.chat.ChatServer
import com.muwire.core.collections.FileCollection
import com.muwire.core.collections.FileCollectionItem
import com.muwire.core.collections.UIDownloadCollectionEvent

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.logging.Level

@Log
public class DownloadManager {

    private final EventBus eventBus
    private final TrustService trustService
    private final MeshManager meshManager
    final MuWireSettings muSettings
    private final I2PConnector connector
    private final Executor executor
    private final File home
    private final Persona me
    private final ChatServer chatServer
    private final FileManager fileManager

    private final Map<InfoHash, Downloader> downloaders = new ConcurrentHashMap<>()
    
    public DownloadManager(EventBus eventBus, TrustService trustService, MeshManager meshManager, MuWireSettings muSettings,
        I2PConnector connector, File home, Persona me, ChatServer chatServer, FileManager fileManager) {
        this.eventBus = eventBus
        this.trustService = trustService
        this.meshManager = meshManager
        this.muSettings = muSettings
        this.connector = connector
        this.home = home
        this.me = me
        this.chatServer = chatServer
        this.fileManager = fileManager

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

        doDownload(infohash, e.target, e.toShare, size, pieceSize, e.sequential, destinations, null)

    }
    
    public void onUIDownloadFeedItemEvent(UIDownloadFeedItemEvent e) {
        Set<Destination> singleSource = new HashSet<>()
        singleSource.add(e.item.getPublisher().getDestination())
        doDownload(e.item.getInfoHash(), e.target, null, e.item.getSize(), e.item.getPieceSize(), 
            e.sequential, singleSource, null)
    }
    
    public void onUIDownloadCollectionEvent(UIDownloadCollectionEvent e) {
        Set<Destination> senderAndAuthor = new HashSet<>()
        senderAndAuthor.add(e.host.destination)
        senderAndAuthor.add(e.collection.author.destination)
        
        e.items.each {
            File target = muSettings.downloadLocation
            for (String pathElement : it.pathElements) {
                target = new File(target, pathElement)
            }

            doDownload(it.infoHash, target, null, it.length, it.pieceSizePow2, e.sequential, senderAndAuthor, e.infoHash)
        }
    }
    
    public void onUIDownloadAttachmentEvent(UIDownloadAttachmentEvent e) {
        Set<Destination> sender = new HashSet<>()
        sender.add(e.sender.destination)
        
        File target = muSettings.downloadLocation
        target = new File(target, e.attachment.name)
        
        doDownload(e.attachment.infoHash, target, null, e.attachment.length, e.attachment.pieceSizePow2, e.sequential, sender, null)
    }
    
    private Downloader doDownload(InfoHash infoHash, File target, File toShare, long size, int pieceSize, 
        boolean sequential, Set<Destination> destinations, InfoHash collectionInfoHash) {
        
        def downloader
        if (fileManager.getRootToFiles().containsKey(infoHash)) {
            def source = fileManager.getRootToFiles().get(infoHash)[0].getFile()
            downloader = new CopyingDownloader(eventBus, this, target, toShare, size, infoHash, 
                    collectionInfoHash, pieceSize, source)
        } else {
            File incompletes = muSettings.incompleteLocation
            if (incompletes == null)
                incompletes = new File(home, "incompletes")
            incompletes.mkdirs()

            Pieces pieces = getPieces(infoHash, size, pieceSize, sequential)
            downloader = new NetworkDownloader(eventBus, this, chatServer, me, target, toShare, size,
                    infoHash, collectionInfoHash, pieceSize, connector, destinations,
                    incompletes, pieces, muSettings.downloadMaxFailures)
        }
        downloaders.put(infoHash, downloader)
        persistDownloaders()
        executor.execute({downloader.download()} as Runnable)
        eventBus.publish(new DownloadStartedEvent(downloader: downloader))
        return downloader
    }

    public void onUIDownloadCancelledEvent(UIDownloadCancelledEvent e) {
        downloaders.remove(e.downloader.infoHash)
        persistDownloaders()
    }

    public void onDownloadHopelessEvent(DownloadHopelessEvent e) {
        downloaders.remove(e.downloader.infoHash)
        persistDownloaders()
    }
    
    public void onUIDownloadPausedEvent(UIDownloadPausedEvent e) {
        persistDownloaders()
    }

    public void onUIDownloadResumedEvent(UIDownloadResumedEvent e) {
        persistDownloaders()
    }

    void onUILoadedEvent(UILoadedEvent e) {
        File downloadsFile = new File(home, "downloads.json")
        if (!downloadsFile.exists())
            return
        def slurper = new JsonSlurper()
        downloadsFile.eachLine {
            def json = slurper.parseText(it)
            File file = new File(DataUtil.readi18nString(Base64.decode(json.file)))
            File toShare = null
            if (json.toShare != null)
                toShare = new File(DataUtil.readi18nString(Base64.decode(json.toShare)))
            
            InfoHash infoHash
            if (json.hashList != null) {
                byte[] hashList = Base64.decode(json.hashList)
                infoHash = InfoHash.fromHashList(hashList)
            } else {
                byte [] root = Base64.decode(json.hashRoot)
                infoHash = new InfoHash(root)
            }
            
            InfoHash collectionInfoHash = null
            if (json.collectionInfoHash != null)
                collectionInfoHash = new InfoHash(Base64.decode(json.collectionInfoHash))
            
            boolean sequential = false
            if (json.sequential != null)
                sequential = json.sequential

            if (json.pieceSizePow2 == null || json.pieceSizePow2 == 0) {
                log.warning("Skipping $file because pieceSizePow2=$json.pieceSizePow2")
                return // skip this download as it's corrupt anyway
            }

            def downloader
            if (json.copying == null) {
                def destinations = new HashSet<>()
                json.destinations.each { destination ->
                    destinations.add new Destination(destination)
                }

                File incompletes
                if (json.incompletes != null)
                    incompletes = new File(DataUtil.readi18nString(Base64.decode(json.incompletes)))
                else
                    incompletes = new File(home, "incompletes")

                
                Pieces pieces = getPieces(infoHash, (long)json.length, json.pieceSizePow2, sequential)

                downloader = new NetworkDownloader(eventBus, this, chatServer, me, file, toShare, (long)json.length,
                    infoHash, collectionInfoHash, json.pieceSizePow2, connector, destinations, incompletes, pieces, muSettings.downloadMaxFailures)
                    downloader.successfulDestinations.addAll(destinations) // if it was persisted, it was successful
                downloader.readPieces()
                if (json.paused != null)
                    downloader.paused = json.paused
            } else {
                File source = new File(DataUtil.readi18nString(Base64.decode(json.source)))
                downloader = new CopyingDownloader(eventBus, this, file, toShare, (long)json.length,
                        infoHash, collectionInfoHash, json.pieceSizePow2, source)
            }
                
                
            try {
                if (!downloader.paused)
                    downloader.download()
                downloaders.put(infoHash, downloader)
                eventBus.publish(new DownloadStartedEvent(downloader : downloader))
            } catch (IllegalArgumentException bad) {
                log.log(Level.WARNING,"cannot start downloader, skipping", bad)
                return
            }
        }
    }

    private Pieces getPieces(InfoHash infoHash, long length, int pieceSizePow2, boolean sequential) {
        long pieceSize = 0x1L << pieceSizePow2
        int nPieces = (int)(length / pieceSize)
        if (length % pieceSize != 0)
            nPieces++
        Mesh mesh = meshManager.getOrCreate(infoHash, nPieces, sequential)
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

    synchronized void persistDownloaders() {
        File downloadsFile = new File(home,"downloads.json")
        downloadsFile.withPrintWriter { writer ->
            downloaders.values().each { downloader ->
                if (!downloader.cancelled) {
                    def json = [:]
                    json.file = Base64.encode(DataUtil.encodei18nString(downloader.file.getAbsolutePath()))
                    if (downloader.toShare != null) {
                        json.toShare = Base64.encode(DataUtil.encodei18nString(downloader.toShare.getAbsolutePath()))
                    }
                    json.length = downloader.length
                    json.pieceSizePow2 = downloader.pieceSizePow2
                    
                    if (downloader instanceof NetworkDownloader) {
                        def destinations = []
                        downloader.destinations.each {
                            destinations << it.toBase64()
                        }
                        json.destinations = destinations
                        json.incompletes = Base64.encode(DataUtil.encodei18nString(downloader.incompletes.getAbsolutePath()))
                    } else if (downloader instanceof CopyingDownloader) {
                        def cd = (CopyingDownloader) downloader
                        json.copying = true
                        json.source = Base64.encode(DataUtil.encodei18nString(cd.source.getAbsolutePath()))
                    }

                    InfoHash infoHash = downloader.getInfoHash()
                    if (infoHash.hashList != null)
                        json.hashList = Base64.encode(infoHash.hashList)
                    else
                        json.hashRoot = Base64.encode(infoHash.getRoot())

                    if (downloader.collectionInfoHash != null)
                        json.collectionInfoHash = Base64.encode(downloader.collectionInfoHash.getRoot())
                        
                    json.paused = downloader.paused
                    json.sequential = downloader.isSequential()
                    
                    writer.println(JsonOutput.toJson(json))
                }
            }
        }
    }

    public void shutdown() {
        downloaders.values().each { it.stop() }
        Downloader.executorService.shutdownNow()
    }
    
    public boolean isDownloading(InfoHash infoHash) {
        downloaders.containsKey(infoHash)
    }
    
    public int totalDownloadSpeed() {
        int total = 0
        downloaders.values().each { total += it.speed() }
        total
    }
}
