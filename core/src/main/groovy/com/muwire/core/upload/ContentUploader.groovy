package com.muwire.core.upload

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.stream.Collectors

import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.mesh.Mesh
import com.muwire.core.util.DataUtil

import net.i2p.data.Destination

class ContentUploader extends Uploader {
    
    private final File file
    private final ContentRequest request
    private final Mesh mesh
    private final int pieceSize
    
    ContentUploader(File file, ContentRequest request, Endpoint endpoint, Mesh mesh, int pieceSize) {
        super(endpoint)
        this.file = file
        this.request = request
        this.mesh = mesh
        this.pieceSize = pieceSize
    }
    
    @Override
    void respond() {
        OutputStream os = endpoint.getOutputStream()
        Range range = request.getRange()
        boolean satisfiable = true
        final long length = file.length()
        if (range.start >= length || range.end >= length)
            satisfiable = false
        if (satisfiable) {
            int startPiece = range.start / (0x1 << pieceSize)
            int endPiece = range.end / (0x1 << pieceSize)
            for (int i = startPiece; i < endPiece; i++)
                satisfiable &= mesh.pieces.isDownloaded(i)
        }
        if (!satisfiable) {
            os.write("416 Range Not Satisfiable\r\n".getBytes(StandardCharsets.US_ASCII))
            writeMesh(request.downloader)
            os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
            os.flush()
            return
        }

        os.write("200 OK\r\n".getBytes(StandardCharsets.US_ASCII))
        os.write("Content-Range: $range.start-$range.end\r\n".getBytes(StandardCharsets.US_ASCII))
        writeMesh(request.downloader)
        os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
        
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
    
    private void writeMesh(Persona toExclude) {
        String xHave = DataUtil.encodeXHave(mesh.pieces.getDownloaded(), mesh.pieces.nPieces)
        endpoint.getOutputStream().write("X-Have: $xHave\r\n".getBytes(StandardCharsets.US_ASCII))
        
        Set<Persona> sources = mesh.getRandom(3, toExclude)
        if (!sources.isEmpty()) {
            String xAlts = sources.stream().map({ it.toBase64() }).collect(Collectors.joining(","))
            endpoint.getOutputStream().write("X-Alt: $xAlts\r\n".getBytes(StandardCharsets.US_ASCII))
        }
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public synchronized int getProgress() {
        if (mapped == null)
            return 0
        int position = mapped.position()
        int total = request.getRange().end - request.getRange().start
        (int)(position * 100.0 / total)
    }

    @Override
    public String getDownloader() {
        request.downloader.getHumanReadableName()
    }

}
