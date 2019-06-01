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
    public enum DownloadState { CONNECTING, DOWNLOADING, FAILED, FINISHED }
    
    private final File file
    private final Pieces pieces
    private final long length
    private final InfoHash infoHash
    private final int pieceSize
    private final I2PConnector connector
    private final Destination destination
    private final int nPieces
    
    private Endpoint endpoint
    private volatile DownloadSession currentSession
    private volatile DownloadState currentState
    
    public Downloader(File file, long length, InfoHash infoHash, int pieceSizePow2, I2PConnector connector, Destination destination) {
        this.file = file
        this.infoHash = infoHash
        this.length = length
        this.connector = connector
        this.destination = destination
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
        Endpoint endpoint = null
        try {
            endpoint = connector.connect(destination)
            currentState = DownloadState.DOWNLOADING
            while(!pieces.isComplete()) {
                currentSession = new DownloadSession(pieces, infoHash, endpoint, file, pieceSize, length)
                currentSession.request()
            }
            currentState = DownloadState.FINISHED
        } catch (Exception bad) {
            log.log(Level.WARNING,"Exception while downloading",bad)
            if (currentState != DownloadState.FINISHED)
                currentState = DownloadState.FAILED
        } finally {
            endpoint?.close()
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
    
    public DownloadState getCurrentState() {
        currentState
    }
}
