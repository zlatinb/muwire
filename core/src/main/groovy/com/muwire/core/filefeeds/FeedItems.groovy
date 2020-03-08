package com.muwire.core.filefeeds

import com.muwire.core.SharedFile
import com.muwire.core.util.DataUtil

import net.i2p.data.Base64

class FeedItems {
    
    public static def sharedFileToObj(SharedFile sf, int certificates) {
        def json = [:]
        json.type = "FeedItem"
        json.version = 1
        json.name = Base64.encode(DataUtil.encodei18nString(sf.getFile().getName()))
        json.infoHash = Base64.encode(sf.getRoot())
        json.size = sf.getCachedLength()
        json.pieceSize = sf.getPieceSize()
        
        if (sf.getComment() != null)
            json.comment = sf.getComment()
        
        json.certificates = certificates
        
        json.timestamp = sf.getPublishedTimestamp()
        
        json
    } 
}
