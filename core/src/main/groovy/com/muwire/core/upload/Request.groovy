package com.muwire.core.upload

import com.muwire.core.InfoHash

import net.i2p.data.Base64

class Request {
    InfoHash infoHash
    Range range
    Map<String, String> headers
    
    static Range parse(InfoHash infoHash, InputStream is) throws IOException {
        
    }
    
}
