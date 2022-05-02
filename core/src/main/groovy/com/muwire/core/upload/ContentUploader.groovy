package com.muwire.core.upload

import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption

import com.muwire.core.connection.Endpoint
import com.muwire.core.mesh.Mesh
import com.muwire.core.util.DataUtil

class ContentUploader extends MeshUploader {

    private final ContentRequest request
    private final int pieceSize
    
    private volatile boolean done

    ContentUploader(File file, ContentRequest request, Endpoint endpoint, Mesh mesh, int pieceSize, boolean confidential) {
        super(file, request, endpoint, mesh, confidential)
        this.request = request
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
            for (int i = startPiece; i <= endPiece; i++)
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
        writeHeadSupport()
        writeConfidential()
        os.write("\r\n".getBytes(StandardCharsets.US_ASCII))

        FileChannel channel = null
        try {
            channel = Files.newByteChannel(file.toPath(), EnumSet.of(StandardOpenOption.READ))
            mapped = channel.map(FileChannel.MapMode.READ_ONLY, range.start, range.end - range.start + 1)
            byte [] tmp = new byte[0x1 << 13]
            while(mapped.hasRemaining()) {
                int read
                synchronized(this) {
                    int start = mapped.position()
                    mapped.get(tmp, 0, Math.min(tmp.length, mapped.remaining()))
                    read = mapped.position() - start
                }
                endpoint.getOutputStream().write(tmp, 0, read)
                dataSinceLastRead.addAndGet(read)
            }
            done = true
        } finally {
            try {channel?.close() } catch (IOException ignored) {}
            synchronized(this) {
                DataUtil.tryUnmap(mapped)
                mapped = null
            }
            endpoint.getOutputStream().flush()
        }
    }

    @Override
    public synchronized int getProgress() {
        if (mapped == null)
            return done ? 100 : 0
        int position = mapped.position()
        int total = request.getRange().end - request.getRange().start
        (int)(position * 100.0d / total)
    }
}
