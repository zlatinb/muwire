package com.muwire.core.download

import com.muwire.core.util.DataUtil

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

import com.muwire.core.Constants
import com.muwire.core.InfoHash
import com.muwire.core.connection.Endpoint

import groovy.util.logging.Log

import static com.muwire.core.util.DataUtil.readTillRN

import net.i2p.data.Base64

@Log
class HashListSession {
    private final String meB64
    private final InfoHash infoHash
    private final Endpoint endpoint
    boolean confidential

    HashListSession(String meB64, InfoHash infoHash, Endpoint endpoint) {
        this.meB64 = meB64
        this.infoHash = infoHash
        this.endpoint = endpoint
    }

    InfoHash request() throws IOException, DownloadRejectedException {
        InputStream is = endpoint.getInputStream()
        OutputStream os = endpoint.getOutputStream()

        String root = Base64.encode(infoHash.getRoot())
        os.write("HASHLIST $root\r\n".getBytes(StandardCharsets.US_ASCII))
        os.write("X-Persona: $meB64\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
        os.flush()

        String code = readTillRN(is)
        if (code.startsWith("429"))
            throw new DownloadRejectedException()
        if (!code.startsWith("200"))
            throw new IOException("unknown code $code")

        // parse all headers
        Map<String, String> headers = DataUtil.readAllHeaders(is)

        if (headers.containsKey("Confidential") && Boolean.parseBoolean(headers["Confidential"]))
            confidential = true
        
        long receivedStart = -1
        long receivedEnd = -1
        String rangeHeader = headers.get("Content-Range")
        if (rangeHeader == null)
            throw new IOException("Content-Range header missing")
        
        def group = (rangeHeader =~ /^(\d+)-(\d+)$/)
        if (group.size() != 1) {
            throw new IOException("Invalid Content-Range header")
        }

        receivedStart = Long.parseLong(group[0][1])
        receivedEnd = Long.parseLong(group[0][2])

        if (receivedStart != 0)
            throw new IOException("hashlist started at $receivedStart")

        byte[] hashList = new byte[receivedEnd]
        ByteBuffer hashListBuf = ByteBuffer.wrap(hashList)
        byte[] tmp = new byte[0x1 << 13]
        while(hashListBuf.hasRemaining()) {
            if (hashListBuf.remaining() > tmp.length)
                tmp = new byte[hashListBuf.remaining()]
            int read = is.read(tmp)
            if (read == -1)
                throw new IOException()
            hashListBuf.put(tmp, 0, read)
        }

        InfoHash received = InfoHash.fromHashList(hashList)
        if (received.getRoot() != infoHash.getRoot())
            throw new IOException("fetched list doesn't match root")
        received
    }
}
