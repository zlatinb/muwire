package com.muwire.core.download;

import net.i2p.data.Base64

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.util.DataUtil

import static com.muwire.core.util.DataUtil.readTillRN

import groovy.util.logging.Log

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.logging.Level

@Log
class DownloadSession {
    
    private static int SAMPLES = 10
    
    private final EventBus eventBus
    private final String meB64
    private final Pieces pieces
    private final InfoHash infoHash
    private final Endpoint endpoint
    private final File file
    private final int pieceSize
    private final long fileLength
    private final Set<Integer> available
    private final MessageDigest digest
    
    private final LinkedList<Long> timestamps = new LinkedList<>()
    private final LinkedList<Integer> reads = new LinkedList<>()
    
    private ByteBuffer mapped
    
    DownloadSession(EventBus eventBus, String meB64, Pieces pieces, InfoHash infoHash, Endpoint endpoint, File file, 
        int pieceSize, long fileLength, Set<Integer> available) {
        this.eventBus = eventBus
        this.meB64 = meB64
        this.pieces = pieces
        this.endpoint = endpoint
        this.infoHash = infoHash
        this.file = file
        this.pieceSize = pieceSize
        this.fileLength = fileLength
        this.available = available
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
        if (available.isEmpty())
            piece = pieces.claim()
        else
            piece = pieces.claim(new HashSet<>(available))
        if (piece == -1)
            return false
        boolean unclaim = true
            
        log.info("will download piece $piece")
        
        long start = piece * pieceSize
        long end = Math.min(fileLength, start + pieceSize) - 1
        long length = end - start + 1
        
        String root = Base64.encode(infoHash.getRoot())
                
        try {
            os.write("GET $root\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Range: $start-$end\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("X-Persona: $meB64\r\n".getBytes(StandardCharsets.US_ASCII))
            String xHave = DataUtil.encodeXHave(pieces.getDownloaded(), pieces.nPieces)
            os.write("X-Have: $xHave\r\n\r\n".getBytes(StandardCharsets.US_ASCII)) 
            os.flush()
            String codeString = readTillRN(is)
            int space = codeString.indexOf(' ')
            if (space > 0)
                codeString = codeString.substring(0, space)
            
            int code = Integer.parseInt(codeString.trim())
            
            if (code == 404) {
                log.warning("file not found")
                endpoint.close()
                return false
            }
                
            if (!(code == 200 || code == 416)) {
                log.warning("unknown code $code")
                endpoint.close()
                return false
            }

            // parse all headers
            Map<String,String> headers = new HashMap<>()
            String header
            while((header = readTillRN(is)) != "" && headers.size() < Constants.MAX_HEADERS) {
                int colon = header.indexOf(':')
                if (colon == -1 || colon == header.length() - 1)
                    throw new IOException("invalid header $header")
                String key = header.substring(0, colon)
                String value = header.substring(colon + 1) 
                headers[key] = value.trim()
            }
            
            // prase X-Alt if present
            if (headers.containsKey("X-Alt")) {
                headers["X-Alt"].split(",").each {
                    if (it.length() > 0) {
                        byte [] raw = Base64.decode(it)
                        Persona source = new Persona(new ByteArrayInputStream(raw))
                        eventBus.publish(new SourceDiscoveredEvent(infoHash : infoHash, source : source))
                    }
                }
            }

            // parse X-Have if present
            if (headers.containsKey("X-Have")) { 
                DataUtil.decodeXHave(headers["X-Have"]).each {
                    available.add(it)
                }
                if (!available.contains(piece))
                    return true // try again next time
            } else {
                if (code != 200)
                    throw new IOException("Code $code but no X-Have")
                available.clear()
            }
            
            if (code != 200)
                return true
            
            String range = headers["Content-Range"]
            if (range == null) 
                throw new IOException("Code 200 but no Content-Range")
            
            def group = (range =~ /^(\d+)-(\d+)$/)
            if (group.size() != 1) 
                throw new IOException("invalid Content-Range header $range")

            long receivedStart = Long.parseLong(group[0][1])
            long receivedEnd = Long.parseLong(group[0][2])

            if (receivedStart != start || receivedEnd != end) {
                log.warning("We don't support mismatching ranges yet")
                endpoint.close()
                return false
            }
            
            // start the download
            FileChannel channel
            try {
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
            } finally {
                try { channel?.close() } catch (IOException ignore) {}
            }
            pieces.markDownloaded(piece)
            unclaim = false
        } finally {
            if (unclaim)
                pieces.unclaim(piece)
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
        if (interval == 0) 
            interval = 1
        for (int i = idx; i < SAMPLES; i++)
            totalRead += reads[idx]
        (int)(totalRead * 1000.0 / interval)
    }
}
