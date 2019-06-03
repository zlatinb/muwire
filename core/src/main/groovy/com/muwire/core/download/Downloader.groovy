package com.muwire.core.download

import com.muwire.core.InfoHash
import com.muwire.core.connection.Endpoint

import java.util.logging.Level

import com.muwire.core.Constants
import com.muwire.core.connection.I2PConnector

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
public class Downloader {
    public enum DownloadState { CONNECTING, DOWNLOADING, FAILED, CANCELLED, FINISHED }

    private final DownloadManager downloadManager 
    private final String meB64   
    private final File file
    private final Pieces pieces
    private final long length
    private final InfoHash infoHash
    private final int pieceSize
    private final I2PConnector connector
    private final Destination destination
    private final int nPieces
    private final File piecesFile
    
    private Endpoint endpoint
    private volatile DownloadSession currentSession
    private volatile DownloadState currentState
    private volatile boolean cancelled
    private volatile Thread downloadThread
    
    public Downloader(DownloadManager downloadManager, String meB64, File file, long length, InfoHash infoHash, 
        int pieceSizePow2, I2PConnector connector, Destination destination,
        File incompletes) {
        this.meB64 = meB64
        this.downloadManager = downloadManager
        this.file = file
        this.infoHash = infoHash
        this.length = length
        this.connector = connector
        this.destination = destination
        this.piecesFile = new File(incompletes, file.getName()+".pieces")
        this.pieceSize = 1 << pieceSizePow2
        
        int nPieces
        if (length % pieceSize == 0)
            nPieces = length / pieceSize
        else
            nPieces = length / pieceSize + 1
        this.nPieces = nPieces
        
        pieces = new Pieces(nPieces, Constants.DOWNLOAD_SEQUENTIAL_RATIO)
        currentState = DownloadState.CONNECTING
    }
    
    void download() {
        readPieces()
        downloadThread = Thread.currentThread()
        Endpoint endpoint = null
        try {
            endpoint = connector.connect(destination)
            currentState = DownloadState.DOWNLOADING
            while(!pieces.isComplete()) {
                currentSession = new DownloadSession(meB64, pieces, infoHash, endpoint, file, pieceSize, length)
                currentSession.request()
                writePieces()
            }
            currentState = DownloadState.FINISHED
            piecesFile.delete()
        } catch (Exception bad) {
            log.log(Level.WARNING,"Exception while downloading",bad)
            if (cancelled)
                currentState = DownloadState.CANCELLED
            else if (currentState != DownloadState.FINISHED)
                currentState = DownloadState.FAILED
        } finally {
            endpoint?.close()
        }
    }
    
    void readPieces() {
        if (!piecesFile.exists())
            return
        piecesFile.withReader { 
            int piece = Integer.parseInt(it.readLine())
            pieces.markDownloaded(piece)
        }
    }
    
    void writePieces() {
        piecesFile.withPrintWriter { writer ->
            pieces.getDownloaded().each { piece -> 
                writer.println(piece)
            }
        }
    }
    
    public long donePieces() {
        pieces.donePieces()
    }
    
    public int positionInPiece() {
        if (currentSession == null)
            return 0
        currentSession.positionInPiece()
    }
    
    public int speed() {
        if (currentSession == null)
            return 0
        currentSession.speed()
    }
    
    public DownloadState getCurrentState() {
        currentState
    }
    
    public void cancel() {
        cancelled = true
        downloadThread?.interrupt()
    }
    
    public void resume() {
        currentState = DownloadState.CONNECTING
        downloadManager.resume(this)
    }
}
