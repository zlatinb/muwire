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

        def state = parseCommonHeaders(headers)
        
        if (state.profile) {
            def dis = new DataInputStream(is)
            int profileLength = dis.readInt()
            if (profileLength > Constants.MAX_PROFILE_LENGTH)
                throw new IOException("Profile too large $profileLength")
            dis.skipBytes(profileLength) // skip for now
        }
        
        new ContentRequest( infoHash : infoHash, range : new Range(start, end),
            headers : headers, downloader : state.downloader, have : state.have,
            browse : state.browse, feed : state.feed, chat : state.chat, message : state.message)
    }
    
    static Request parseHeadRequest(InfoHash infoHash, InputStream is) throws IOException {
        Map<String, String> headers = DataUtil.readAllHeaders(is)
        def state = parseCommonHeaders(headers)
        new HeadRequest(infoHash: infoHash, headers: headers, downloader: state.downloader, have: state.have,
            browse: state.browse, feed: state.feed, chat: state.chat, message: state.message)
    }
    
    private static RequestParsingState parseCommonHeaders(Map<String,String> headers) throws IOException {
        RequestParsingState rv = new RequestParsingState()
        
        rv.browse = headers.containsKey("Browse") && Boolean.parseBoolean(headers['Browse'])
        rv.feed = headers.containsKey("Feed") && Boolean.parseBoolean(headers['Feed'])
        rv.chat = headers.containsKey("Chat") && Boolean.parseBoolean(headers['Chat'])
        rv.message = headers.containsKey("Message") && Boolean.parseBoolean(headers['Message'])
        rv.profile = headers.containsKey("Profile") && Boolean.parseBoolean(headers['Profile'])
        
        if (headers.containsKey("X-Have")) {
            def encoded = headers["X-Have"].trim()
            rv.have = DataUtil.decodeXHave(encoded).size()
        }
        
        if (headers.containsKey("X-Persona")) {
            def encoded = headers["X-Persona"].trim()
            def decoded = Base64.decode(encoded)
            rv.downloader = new Persona(new ByteArrayInputStream(decoded))
        }
        
        rv
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
    
    private static class RequestParsingState {
        Persona downloader
        boolean browse, feed, chat, message, profile
        int have
    }
}
