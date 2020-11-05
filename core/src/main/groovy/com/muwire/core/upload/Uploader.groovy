package com.muwire.core.upload

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicInteger

import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint

abstract class Uploader {
    protected final Endpoint endpoint
    protected ByteBuffer mapped
    
    protected final AtomicInteger dataSinceLastRead = new AtomicInteger()

    Uploader(Endpoint endpoint) {
        this.endpoint = endpoint
    }

    abstract void respond()

    public synchronized int getPosition() {
        if (mapped == null)
            return -1
        mapped.position()
    }

    abstract String getName();
    

    /**
     * @return an integer between 0 and 100
     */
    abstract int getProgress();

    abstract String getDownloader();
    abstract Persona getDownloaderPersona();

    abstract int getDonePieces();

    abstract int getTotalPieces();

    abstract long getTotalSize();
    
    abstract boolean isBrowseEnabled();
    abstract boolean isFeedEnabled();
    abstract boolean isChatEnabled();
    abstract boolean isMessageEnabled();
    
    int dataSinceLastRead() {
        dataSinceLastRead.getAndSet(0)
    }
}
