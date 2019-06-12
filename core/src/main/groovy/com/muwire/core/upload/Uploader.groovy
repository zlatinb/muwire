package com.muwire.core.upload

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption

import com.muwire.core.connection.Endpoint

abstract class Uploader {
    protected final Endpoint endpoint
    protected ByteBuffer mapped
    
    Uploader(Endpoint endpoint) {
        this.endpoint = endpoint
    }
    
    abstract void respond()
        
    public synchronized int getPosition() {
        if (mapped == null)
            return -1
        mapped.position()
    }
}
