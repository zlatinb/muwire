package com.muwire.core.download

import com.muwire.core.DownloadedFile
import com.muwire.core.InfoHash
import com.muwire.core.files.FileDownloadedEvent

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import com.muwire.core.EventBus

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
abstract class Downloader {
    
    enum DownloadState { COPYING, CONNECTING, HASHLIST, DOWNLOADING, REJECTED, FAILED, HOPELESS, CANCELLED, PAUSED, FINISHED }

    protected static final ExecutorService executorService = Executors.newCachedThreadPool({r ->
        Thread rv = new Thread(r)
        rv.setName("download worker")
        rv.setDaemon(true)
        rv
    })

    protected final EventBus eventBus
    protected final DownloadManager downloadManager
    
    protected final File file
    protected final File toShare
    protected final long length
    
    protected volatile InfoHash infoHash, collectionInfoHash
    protected final int pieceSize, pieceSizePow2
    private final int nPieces
    
    protected volatile boolean cancelled, paused
    

    protected Downloader(EventBus eventBus, DownloadManager downloadManager, File file, File toShare, 
                         long length, InfoHash infoHash,
                        InfoHash collectionInfoHash, int pieceSizePow2) {
        this.eventBus = eventBus
        this.downloadManager = downloadManager
        this.file = file
        this.toShare = toShare
        this.infoHash = infoHash
        this.collectionInfoHash = collectionInfoHash
        this.length = length
        this.pieceSizePow2 = pieceSizePow2
        this.pieceSize = 1 << pieceSizePow2

        int nPieces = (int)(length / pieceSize)
        if (length % pieceSize != 0)
            nPieces++
        this.nPieces = nPieces
    }

    InfoHash getInfoHash() {
        infoHash
    }
    
    public File getFile() {
        file
    }
    
    public int getNPieces() {
        nPieces
    }

    public int getPieceSize() {
        pieceSize
    }    
    
    public long getLength() {
        length
    }

    abstract void download();

    abstract long donePieces();

    abstract int speed();
    
    abstract boolean isPausable();
    
    abstract boolean isConfidential();

    DownloadState getCurrentState() {
        if (cancelled)
            return DownloadState.CANCELLED
        if (paused)
            return DownloadState.PAUSED
        getSpecificState()
    }
    
    protected abstract DownloadState getSpecificState();
    
    void cancel() {
        cancelled = true;
        doCancel();
    }
    
    protected abstract void doCancel();

    void pause() {
        paused = true
        doPause()
    }

    protected abstract void doPause();
    
    protected abstract void stop();
    
    abstract int activeWorkers();
    
    abstract int getTotalWorkers();
    
    abstract int countHopelessSources();

    public void resume() {
        paused = false
        doResume()
    }
    
    protected abstract void doResume();

    abstract void addSource(Destination d);
    
    abstract boolean isSequential();
    
    abstract File generatePreview();
    
    protected void fireEvent(Set<Destination> successfulDestinations, boolean confidential) {
        def event = new FileDownloadedEvent(
                downloadedFile: new DownloadedFile(file.getCanonicalFile(), infoHash.getRoot(), 
                        pieceSizePow2, successfulDestinations),
                parentToShare: toShare,
                downloader: this,
                infoHash: infoHash,
                collectionInfoHash: collectionInfoHash,
                confidential: confidential)
        eventBus.publish event
    }

}
