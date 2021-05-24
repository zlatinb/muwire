package com.muwire.core.download

import com.muwire.core.util.DataUtil

import java.nio.charset.StandardCharsets

class SessionSupport {
    static void writeInteractionHeaders(OutputStream os, boolean browse, boolean chat,
        boolean feed, boolean message) throws IOException {
        if (browse)
            os.write("Browse: true\r\n".getBytes(StandardCharsets.US_ASCII))
        if (feed)
            os.write("Feed: true\r\n".getBytes(StandardCharsets.US_ASCII))
        if (chat)
            os.write("Chat: true\r\n".getBytes(StandardCharsets.US_ASCII))
        if (message)
            os.write("Message: true\r\n".getBytes(StandardCharsets.US_ASCII))
    }
    
    static void writeXHave(OutputStream os, Pieces pieces) throws IOException  {
        String xHave = DataUtil.encodeXHave(pieces.getDownloaded(), pieces.nPieces)
        os.write("X-Have: $xHave\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
    }
}
