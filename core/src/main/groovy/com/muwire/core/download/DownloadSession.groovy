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
    
    private static int SAMPLES = 10
    
    private final String meB64
    private final Pieces downloaded, claimed
    private final InfoHash infoHash
    private final Endpoint endpoint
    private final File file
    private final int pieceSize
    private final long fileLength
    private final MessageDigest digest
    
    private final LinkedList<Long> timestamps = new LinkedList<>()
    private final LinkedList<Integer> reads = new LinkedList<>()
    
    private ByteBuffer mapped
    
    DownloadSession(String meB64, Pieces downloaded, Pieces claimed, InfoHash infoHash, Endpoint endpoint, File file, 
        int pieceSize, long fileLength) {
        this.meB64 = meB64
        this.downloaded = downloaded
        this.claimed = claimed
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
    
    /**
     * @return if the request will proceed.  The only time it may not
     * is if all the pieces have been claimed by other sessions.
     * @throws IOException
     */
    public boolean request() throws IOException {
        OutputStream os = endpoint.getOutputStream()
        InputStream is = endpoint.getInputStream()
        
        int piece
        while(true) {
            piece = downloaded.getRandomPiece()
            if (claimed.isMarked(piece)) {
                if (downloaded.donePieces() + claimed.donePieces() == downloaded.nPieces) {
                    log.info("all pieces claimed")
                    return false
                }
                continue
            }
            break
        }
        claimed.markDownloaded(piece)
        
        log.info("will download piece $piece")
        
        long start = piece * pieceSize
        long end = Math.min(fileLength, start + pieceSize) - 1
        long length = end - start + 1
        
        String root = Base64.encode(infoHash.getRoot())
                
        FileChannel channel
        try {
            os.write("GET $root\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Range: $start-$end\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("X-Persona: $meB64\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
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
                if (mapped.remaining() < tmp.length)
                    tmp = new byte[mapped.remaining()]
                int read = is.read(tmp)
                if (read == -1)
                    throw new IOException()
                synchronized(this) {
                    mapped.put(tmp, 0, read)
                    
                    if (timestamps.size() == SAMPLES) {
                        timestamps.removeFirst()
                        reads.removeFirst()
                    }
                    timestamps.addLast(System.currentTimeMillis())
                    reads.addLast(read)
                }
            }
            
            mapped.clear()
            digest.update(mapped)
            byte [] hash = digest.digest()
            byte [] expected = new byte[32]
            System.arraycopy(infoHash.getHashList(), piece * 32, expected, 0, 32)    
            if (hash != expected) 
                throw new BadHashException()
            
            downloaded.markDownloaded(piece)        
        } finally {
            claimed.clear(piece)
            try { channel?.close() } catch (IOException ignore) {}
        }
        return true
    }
    
    synchronized int positionInPiece() {
        if (mapped == null)
            return 0
        mapped.position()
    }
    
    synchronized int speed() {
        if (timestamps.size() < SAMPLES)
            return 0
        int totalRead = 0
        int idx = 0
        final long now = System.currentTimeMillis()
        
        while(idx < SAMPLES && timestamps.get(idx) < now - 1000)
            idx++
        if (idx == SAMPLES)
            return 0
        if (idx == SAMPLES - 1)
            return reads[idx]
            
        long interval = timestamps.last - timestamps[idx]
        
        for (int i = idx; i < SAMPLES; i++)
            totalRead += reads[idx]
        (int)(totalRead * 1000.0 / interval)
    }
}
