package com.muwire.core.upload

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption

import com.muwire.core.connection.Endpoint

class Uploader {
    private final File file
    private final Request request
    private final Endpoint endpoint
    private ByteBuffer mapped
    
    Uploader(File file, Request request, Endpoint endpoint) {
        this.file = file
        this.request = request
        this.endpoint = endpoint
    }
    
    void respond() {
        OutputStream os = endpoint.getOutputStream()
        Range range = request.getRange()
        if (range.start >= file.length() || range.end >= file.length()) {
            os.write("416 Range Not Satisfiable\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
            os.flush()
            return
        }

        os.write("200 OK\r\n".getBytes(StandardCharsets.US_ASCII))
        os.write("Content-Range: $range.start-$range.end\r\n\r\n".getBytes(StandardCharsets.US_ASCII))        

        FileChannel channel
        try {
            channel = Files.newByteChannel(file.toPath(), EnumSet.of(StandardOpenOption.READ))
            mapped = channel.map(FileChannel.MapMode.READ_ONLY, range.start, range.end - range.start + 1)
            byte [] tmp = new byte[0x1 << 13]
            while(mapped.hasRemaining()) {
                int start = mapped.position()
                synchronized(this) {
                    mapped.get(tmp, 0, Math.min(tmp.length, mapped.remaining()))
                }
                int read = mapped.position() - start
                endpoint.getOutputStream().write(tmp, 0, read)
            }
        } finally {
            try {channel?.close() } catch (IOException ignored) {}
            endpoint.getOutputStream().flush()
        }     
    }
    
    public synchronized int getPosition() {
        if (mapped == null)
            return -1
        mapped.position()
    }
}
