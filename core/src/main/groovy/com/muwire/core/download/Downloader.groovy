package com.muwire.core.download

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

import com.muwire.core.Constants
import com.muwire.core.DownloadedFile
import com.muwire.core.EventBus
import com.muwire.core.connection.I2PConnector
import com.muwire.core.files.FileDownloadedEvent

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
public class Downloader {
    public enum DownloadState { CONNECTING, HASHLIST, DOWNLOADING, FAILED, CANCELLED, FINISHED }
    private enum WorkerState { CONNECTING, HASHLIST, DOWNLOADING, FINISHED}
    
    private static final ExecutorService executorService = Executors.newCachedThreadPool({r ->
        Thread rv = new Thread(r)
        rv.setName("download worker")
        rv.setDaemon(true)
        rv
    })

    private final EventBus eventBus
    private final DownloadManager downloadManager 
    private final Persona me   
    private final File file
    private final Pieces pieces
    private final long length
    private InfoHash infoHash
    private final int pieceSize
    private final I2PConnector connector
    private final Set<Destination> destinations
    private final int nPieces
    private final File piecesFile
    private final File incompleteFile
    final int pieceSizePow2
    private final Map<Destination, DownloadWorker> activeWorkers = new ConcurrentHashMap<>()
    
    
    private volatile boolean cancelled
    private final AtomicBoolean eventFired = new AtomicBoolean()
    private boolean piecesFileClosed

    public Downloader(EventBus eventBus, DownloadManager downloadManager, 
        Persona me, File file, long length, InfoHash infoHash, 
        int pieceSizePow2, I2PConnector connector, Set<Destination> destinations,
        File incompletes) {
        this.eventBus = eventBus
        this.me = me
        this.downloadManager = downloadManager
        this.file = file
        this.infoHash = infoHash
        this.length = length
        this.connector = connector
        this.destinations = destinations
        this.piecesFile = new File(incompletes, file.getName()+".pieces")
        this.incompleteFile = new File(incompletes, file.getName()+".part")
        this.pieceSizePow2 = pieceSizePow2
        this.pieceSize = 1 << pieceSizePow2
        
        int nPieces
        if (length % pieceSize == 0)
            nPieces = length / pieceSize
        else
            nPieces = length / pieceSize + 1
        this.nPieces = nPieces
        
        pieces = new Pieces(nPieces, Constants.DOWNLOAD_SEQUENTIAL_RATIO)
    }
    
    public synchronized InfoHash getInfoHash() {
        infoHash
    }
    
    private synchronized void setInfoHash(InfoHash infoHash) {
        this.infoHash = infoHash
    }
    
    void download() {
        readPieces()
        destinations.each {
            if (it != me.destination) {
                def worker = new DownloadWorker(it)
                activeWorkers.put(it, worker)
                executorService.submit(worker)
            }
        }
    }
    
    void readPieces() {
        if (!piecesFile.exists())
            return
        piecesFile.eachLine { 
            int piece = Integer.parseInt(it)
            pieces.markDownloaded(piece)
        }
    }
    
    void writePieces() {
        synchronized(piecesFile) {
            if (piecesFileClosed)
                return
            piecesFile.withPrintWriter { writer ->
                pieces.getDownloaded().each { piece ->
                    writer.println(piece)
                }
            }
        }
    }
    
    public long donePieces() {
        pieces.donePieces()
    }
    
    
    public int speed() {
        int total = 0
        if (getCurrentState() == DownloadState.DOWNLOADING) {
            activeWorkers.values().each {
                if (it.currentState == WorkerState.DOWNLOADING)
                    total += it.speed()
            }
        }
        total
    }
    
    public DownloadState getCurrentState() {
        if (cancelled)
            return DownloadState.CANCELLED
        boolean allFinished = true
        activeWorkers.values().each { 
            allFinished &= it.currentState == WorkerState.FINISHED
        }
        if (allFinished) {
            if (pieces.isComplete())
                return DownloadState.FINISHED
            return DownloadState.FAILED
        }
        
        // if at least one is downloading...
        boolean oneDownloading = false
        activeWorkers.values().each { 
            if (it.currentState == WorkerState.DOWNLOADING) {
                oneDownloading = true
                return 
            }
        }
        
        if (oneDownloading)
            return DownloadState.DOWNLOADING
        
        // at least one is requesting hashlist
        boolean oneHashlist = false
        activeWorkers.values().each { 
            if (it.currentState == WorkerState.HASHLIST) {
                oneHashlist = true
                return
            }
        }
        if (oneHashlist)
            return DownloadState.HASHLIST
            
        return DownloadState.CONNECTING
    }
    
    public void cancel() {
        cancelled = true
        stop()
        synchronized(piecesFile) {
            piecesFileClosed = true
            piecesFile.delete()
        }
        incompleteFile.delete()
    }
    
    void stop() {
        activeWorkers.values().each { 
            it.cancel()
        }
    }
    
    public int activeWorkers() {
        int active = 0
        activeWorkers.values().each { 
            if (it.currentState != WorkerState.FINISHED)
                active++
        }
        active
    }
    
    public void resume() {
        destinations.each { destination ->
            def worker = activeWorkers.get(destination)
            if (worker != null) {
                if (worker.currentState == WorkerState.FINISHED) {
                    def newWorker = new DownloadWorker(destination)
                    activeWorkers.put(destination, newWorker)
                    executorService.submit(newWorker)
                }
            } else {
                worker = new DownloadWorker(destination)
                activeWorkers.put(destination, worker)
                executorService.submit(worker)
            }
        }
    }
    
    class DownloadWorker implements Runnable {
        private final Destination destination
        private volatile WorkerState currentState
        private volatile Thread downloadThread
        private Endpoint endpoint
        private volatile DownloadSession currentSession
                
        DownloadWorker(Destination destination) {
            this.destination = destination
        }
        
        public void run() {
            downloadThread = Thread.currentThread()
            currentState = WorkerState.CONNECTING
            Endpoint endpoint = null
            try {
                endpoint = connector.connect(destination)
                while(getInfoHash().hashList == null) {
                    currentState = WorkerState.HASHLIST
                    HashListSession session = new HashListSession(me.toBase64(), infoHash, endpoint)
                    InfoHash received = session.request()
                    setInfoHash(received)
                }
                currentState = WorkerState.DOWNLOADING
                boolean requestPerformed
                while(!pieces.isComplete()) {
                    currentSession = new DownloadSession(me.toBase64(), pieces, getInfoHash(), endpoint, incompleteFile, pieceSize, length)
                    requestPerformed = currentSession.request()
                    if (!requestPerformed)
                        break
                    writePieces()
                }
            } catch (Exception bad) {
                log.log(Level.WARNING,"Exception while downloading",bad)
            } finally {
                currentState = WorkerState.FINISHED
                if (pieces.isComplete() && eventFired.compareAndSet(false, true)) {
                    synchronized(piecesFile) {
                        piecesFileClosed = true
                        piecesFile.delete()
                    }
                    try {
                        Files.move(incompleteFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.copy(incompleteFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        incompleteFile.delete()
                    }
                    eventBus.publish(
                        new FileDownloadedEvent(
                            downloadedFile : new DownloadedFile(file, getInfoHash(), pieceSizePow2, Collections.emptySet()),
                        downloader : Downloader.this))
                            
                } 
                endpoint?.close()
            }
        }
        
        int speed() {
            if (currentSession == null)
                return 0
            currentSession.speed()
        }
        
        void cancel() {
            downloadThread?.interrupt()
        }
    }
}
