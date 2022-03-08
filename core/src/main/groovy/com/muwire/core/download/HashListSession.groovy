package com.muwire.core.download

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
