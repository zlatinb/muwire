package com.muwire.core.download;

import net.i2p.data.Base64

import com.muwire.core.Constants
import com.muwire.core.InfoHash
import com.muwire.core.connection.Endpoint
import static com.muwire.core.util.DataUtil.readTillRN

import groovy.util.logging.Log

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

@Log
class DownloadSession {
    
    private final Pieces pieces
    private final InfoHash infoHash
    private final Endpoint endpoint
    private final File file
    private final int pieceSize
    private final long fileLength
    private final MessageDigest digest
    
    private ByteBuffer mapped
    
    DownloadSession(Pieces pieces, InfoHash infoHash, Endpoint endpoint, File file, 
        int pieceSize, long fileLength) {
        this.pieces = pieces
        this.endpoint = endpoint
        this.infoHash = infoHash
        this.file = file
        this.pieceSize = pieceSize
        this.fileLength = fileLength
        try {
            digest = MessageDigest.getInstance("SHA-256")
        } catch (NoSuchAlgorithmException impossible) {
            digest = null
            System.exit(1)
        }
    }
    
    public void request() throws IOException {
        OutputStream os = endpoint.getOutputStream()
        InputStream is = endpoint.getInputStream()
        
        int piece = pieces.getRandomPiece()
        long start = piece * pieceSize
        long end = Math.min(fileLength, start + pieceSize) - 1
        long length = end - start + 1
        
        String root = Base64.encode(infoHash.getRoot())
                
        FileChannel channel
        try {
            os.write("GET $root\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Range: $start-$end\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
            os.flush()
            String code = readTillRN(is)
            if (code.startsWith("404 ")) {
                log.warning("file not found")
                endpoint.close()
                return
            }
                
            if (code.startsWith("416 ")) {
                log.warning("range $start-$end cannot be satisfied")
                return // leave endpoint open
            }
            
            if (!code.startsWith("200 ")) {
                log.warning("unknown code $code")
                endpoint.close()
                return
            }

            // parse all headers
            Set<String> headers = new HashSet<>()
            String header
            while((header = readTillRN(is)) != "" && headers.size() < Constants.MAX_HEADERS) 
                headers.add(header)

            long receivedStart = -1
            long receivedEnd = -1
            for (String receivedHeader : headers) {
                def group = (receivedHeader =~ /^Content-Range: (\d+)-(\d+)$/)
                if (group.size() != 1) {
                    log.info("ignoring header $receivedHeader")
                    continue
                } 

                receivedStart = Long.parseLong(group[0][1])
                receivedEnd = Long.parseLong(group[0][2])
            }
            
            if (receivedStart != start || receivedEnd != end) {
                log.warning("We don't support mismatching ranges yet")
                endpoint.close()
                return
            }
            
            // start the download
            channel = Files.newByteChannel(file.toPath(), EnumSet.of(StandardOpenOption.READ, StandardOpenOption.WRITE, 
                StandardOpenOption.SPARSE, StandardOpenOption.CREATE)) // TODO: double-check, maybe CREATE_NEW
            mapped = channel.map(FileChannel.MapMode.READ_WRITE, start, end - start + 1)
            
            byte[] tmp = new byte[0x1 << 13]
            while(mapped.hasRemaining()) {
                int read = is.read(tmp)
                if (read == -1)
                    throw new IOException()
                synchronized(this) {
                    mapped.put(tmp, 0, read)
                }
            }
            
            mapped.clear()
            digest.update(mapped)
            byte [] hash = digest.digest()
            byte [] expected = new byte[32]
            System.arraycopy(infoHash.getHashList(), piece * 32, expected, 0, 32)    
            if (hash != expected) {
                log.warning("hash mismatch")
                endpoint.close()
                return
            }
            
            pieces.markDownloaded(piece)        
        } finally {
            try { channel?.close() } catch (IOException ignore) {}
        }
    }
}
