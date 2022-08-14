package com.muwire.core.filefeeds

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.SharedFile
import com.muwire.core.files.FileHasher
import com.muwire.core.util.DataUtil

import net.i2p.data.Base64

class FeedItems {
    
    public static def sharedFileToObj(SharedFile sf, int certificates) {
        def json = [:]
        json.type = "FeedItem"
        json.version = 1
        json.name = Base64.encode(DataUtil.encodei18nString(sf.getFile().getName()))
        json.infoHash = Base64.encode(sf.getRoot())
        json.size = sf.getFile().length()
        json.pieceSize = sf.getPieceSize()
        
        if (sf.getComment() != null)
            json.comment = sf.getComment()
        
        json.certificates = certificates
        
        json.timestamp = sf.getPublishedTimestamp()
        
        json
    } 
    
    public static FeedItem objToFeedItem(def obj, Persona publisher) throws InvalidFeedItemException {
        if (obj.timestamp == null)
            throw new InvalidFeedItemException("No timestamp");
        if (obj.name == null)
            throw new InvalidFeedItemException("No name");
        if (obj.size == null || obj.size <= 0 || obj.size > FileHasher.MAX_SIZE)
            throw new InvalidFeedItemException("length missing or invalid ${obj.size}")
        if (obj.pieceSize == null || obj.pieceSize < FileHasher.MIN_PIECE_SIZE_POW2 || obj.pieceSize > FileHasher.MAX_PIECE_SIZE_POW2)
            throw new InvalidFeedItemException("piece size missing or invalid ${obj.pieceSize}")
        if (obj.infoHash == null)
            throw new InvalidFeedItemException("Infohash missing")
        
            
        InfoHash infoHash
        try {
            infoHash = new InfoHash(Base64.decode(obj.infoHash))
        } catch (Exception bad) {
            throw new InvalidFeedItemException("Invalid infohash", bad)
        }
        
        String name
        try {
            name = DataUtil.readi18nString(Base64.decode(obj.name))
        } catch (Exception bad) {
            throw new InvalidFeedItemException("Invalid name", bad)
        }
        
        int certificates = 0
        if (obj.certificates != null)
            certificates = obj.certificates
        
        new FeedItem(publisher, obj.timestamp, name, obj.size, obj.pieceSize, infoHash, certificates, obj.comment)
    }
    
    public static def feedItemToObj(FeedItem item) {
        def json = [:]
        json.type = "FeedItem"
        json.version = 1
        json.name = Base64.encode(DataUtil.encodei18nString(item.getName()))
        json.infoHash = Base64.encode(item.getInfoHash().getRoot())
        json.size = item.getSize()
        json.pieceSize = item.getPieceSize()
        json.timestamp = item.getTimestamp()
        json.certificates = item.getCertificates()
        json.comment = item.getComment()
        json
    }
}
