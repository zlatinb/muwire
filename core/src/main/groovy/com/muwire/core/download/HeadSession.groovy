package com.muwire.core.download

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.util.DataUtil
import groovy.util.logging.Log
import net.i2p.data.Base64

import java.nio.charset.StandardCharsets

import static com.muwire.core.util.DataUtil.readTillRN

@Log
class HeadSession {
    private final EventBus eventBus
    private final String meB64
    private final Pieces pieces
    private final InfoHash infoHash
    private final Endpoint endpoint
    private final boolean browse, feed, chat, message
    
    HeadSession(EventBus eventBus, String meB64, Pieces pieces, InfoHash infoHash, Endpoint endpoint,
        boolean browse, boolean feed, boolean chat, boolean message) {
        this.eventBus = eventBus
        this.meB64 = meB64
        this.pieces = pieces
        this.infoHash = infoHash
        this.endpoint = endpoint
        this.browse = browse
        this.feed = feed
        this.chat = chat
        this.message = message
    }
    
    void performRequest() throws IOException {
        OutputStream os = endpoint.getOutputStream()
        String root = Base64.encode(infoHash.getRoot())
        
        log.info("performing HEAD request for root $root")
        os.write("HEAD $root\r\n".getBytes(StandardCharsets.US_ASCII))
        os.write("X-Persona: $meB64\r\n".getBytes(StandardCharsets.US_ASCII))
        SessionSupport.writeInteractionHeaders(os, browse, chat, feed, message)
        SessionSupport.writeXHave(os, pieces)
        os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
        os.flush()
        
        InputStream is = endpoint.getInputStream()
        String codeString = readTillRN(is)
        int space = codeString.indexOf(' ')
        if (space > 0)
            codeString = codeString.substring(0, space)

        int code = Integer.parseInt(codeString.trim())
        
        if (code != 200) {
            log.warning("unknown code for HEAD request $code")
            endpoint.close()
            return
        }

        // parse all headers
        Map<String,String> headers = DataUtil.readAllHeaders(is)

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
        
        // don't care about the X-Have for now
    }
}
