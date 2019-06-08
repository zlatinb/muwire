package com.muwire.core.download

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
    public enum DownloadState { CONNECTING, DOWNLOADING, FAILED, CANCELLED, FINISHED }
    private enum WorkerState { CONNECTING, DOWNLOADING, FINISHED}
    
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
    private final Pieces downloaded, claimed
    private final long length
    private final InfoHash infoHash
    private final int pieceSize
    private final I2PConnector connector
    private final Set<Destination> destinations
    private final int nPieces
    private final File piecesFile
    private final Map<Destination, DownloadWorker> activeWorkers = new ConcurrentHashMap<>()
    
    
    private volatile boolean cancelled
    private volatile boolean eventFired

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
        this.pieceSize = 1 << pieceSizePow2
        
        int nPieces
        if (length % pieceSize == 0)
            nPieces = length / pieceSize
        else
            nPieces = length / pieceSize + 1
        this.nPieces = nPieces
        
        downloaded = new Pieces(nPieces, Constants.DOWNLOAD_SEQUENTIAL_RATIO)
        claimed = new Pieces(nPieces)
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
        piecesFile.withReader { 
            int piece = Integer.parseInt(it.readLine())
            downloaded.markDownloaded(piece)
        }
    }
    
    void writePieces() {
        piecesFile.withPrintWriter { writer ->
            downloaded.getDownloaded().each { piece -> 
                writer.println(piece)
            }
        }
    }
    
    public long donePieces() {
        downloaded.donePieces()
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
            if (downloaded.isComplete())
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
        
        return DownloadState.CONNECTING
    }
    
    public void cancel() {
        cancelled = true
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
        activeWorkers.each { destination, worker ->
            if (worker.currentState == WorkerState.FINISHED) {
                def newWorker = new DownloadWorker(destination)
                activeWorkers.put(destination, newWorker)
                executorService.submit(newWorker)
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
                currentState = WorkerState.DOWNLOADING
                boolean requestPerformed
                while(!downloaded.isComplete()) {
                    currentSession = new DownloadSession(me.toBase64(), downloaded, claimed, infoHash, endpoint, file, pieceSize, length)
                    requestPerformed = currentSession.request()
                    if (!requestPerformed)
                        break
                    writePieces()
                }
            } catch (Exception bad) {
                log.log(Level.WARNING,"Exception while downloading",bad)
            } finally {
                currentState = WorkerState.FINISHED
                if (downloaded.isComplete() && !eventFired) {
                    piecesFile.delete()
                    eventFired = true
                    eventBus.publish(new FileDownloadedEvent(downloadedFile : new DownloadedFile(file, infoHash, pieceSize, Collections.emptySet())))
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
