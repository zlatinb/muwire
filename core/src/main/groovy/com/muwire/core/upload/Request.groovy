package com.muwire.core.upload

import java.nio.charset.StandardCharsets

import com.muwire.core.Constants
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.util.DataUtil

import groovy.util.logging.Log
import net.i2p.data.Base64

@Log
class Request {

    private static final byte R = "\r".getBytes(StandardCharsets.US_ASCII)[0]
    private static final byte N = "\n".getBytes(StandardCharsets.US_ASCII)[0]

    InfoHash infoHash
    Persona downloader
    Map<String, String> headers

    static Request parseContentRequest(InfoHash infoHash, InputStream is) throws IOException {

        Map<String, String> headers = DataUtil.readAllHeaders(is)

        if (!headers.containsKey("Range"))
            throw new IOException("Range header not found")

        String range = headers.get("Range").trim()
        String[] split = range.split("-")
        if (split.length != 2)
            throw new IOException("Invalid range header $range")
        long start
        long end
        try {
            start = Long.parseLong(split[0])
            end = Long.parseLong(split[1])
        } catch (NumberFormatException nfe) {
            throw new IOException(nfe)
        }

        if (start < 0 || end < start)
            throw new IOException("Invalid range $start - $end")

        Persona downloader = null
        if (headers.containsKey("X-Persona")) {
            def encoded = headers["X-Persona"].trim()
            def decoded = Base64.decode(encoded)
            downloader = new Persona(new ByteArrayInputStream(decoded))
        }

        int have = 0
        if (headers.containsKey("X-Have")) {
            def encoded = headers["X-Have"].trim()
            have = DataUtil.decodeXHave(encoded).size()
        }
        
        boolean browse = headers.containsKey("Browse") && Boolean.parseBoolean(headers['Browse'])
        boolean feed = headers.containsKey("Feed") && Boolean.parseBoolean(headers['Feed'])
        boolean chat = headers.containsKey("Chat") && Boolean.parseBoolean(headers['Chat'])
        
        new ContentRequest( infoHash : infoHash, range : new Range(start, end),
            headers : headers, downloader : downloader, have : have,
            browse : browse, feed : feed, chat : chat)
    }

    static Request parseHashListRequest(InfoHash infoHash, InputStream is) throws IOException {
        Map<String,String> headers = DataUtil.readAllHeaders(is)
        Persona downloader = null
        if (headers.containsKey("X-Persona")) {
            def encoded = headers["X-Persona"].trim()
            def decoded = Base64.decode(encoded)
            downloader = new Persona(new ByteArrayInputStream(decoded))
        }
        new HashListRequest(infoHash : infoHash, headers : headers, downloader : downloader)
    }
}
