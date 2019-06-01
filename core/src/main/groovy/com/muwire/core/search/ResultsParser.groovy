package com.muwire.core.search

import javax.naming.directory.InvalidSearchControlsException

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.util.DataUtil

import net.i2p.data.Base64

class ResultsParser {
    public static UIResultEvent parse(Persona p, UUID uuid, def json) throws InvalidSearchResultException {
        if (json.type != "Result")
            throw new InvalidSearchResultException("not a result json")
        if (json.version != 1)
            throw new InvalidSearchResultException("unknown version $json.version")
        if (json.name == null)
            throw new InvalidSearchResultException("name missing")
        if (json.size == null)
            throw new InvalidSearchResultException("length missing")
        if (json.infohash == null)
            throw new InvalidSearchResultException("infohash missing")
        if (json.pieceSize == null)
            throw new InvalidSearchResultException("pieceSize missing")
        if (!(json.hashList instanceof List))
            throw new InvalidSearchResultException("hashlist not a list")
        try {
            String name = DataUtil.readi18nString(Base64.decode(json.name))
            long size = json.size
            byte [] infoHash = Base64.decode(json.infohash)
            if (infoHash.length != InfoHash.SIZE)
                throw new InvalidSearchResultException("invalid infohash size $infoHash.length")
            int pieceSize = json.pieceSize
            byte [] hashList = new byte[json.hashList.size() * InfoHash.SIZE]
            json.hashList.eachWithIndex { string, index ->
                byte [] hashPiece = Base64.decode(string)
                if (hashPiece.length != InfoHash.SIZE)
                    throw new InvalidSearchResultException("Invalid piece hash size $hashPiece.length at index $index")
                System.arraycopy(hashPiece, 0, hashList, index * InfoHash.SIZE, InfoHash.SIZE)
            }
            InfoHash parsedIH = InfoHash.fromHashList(hashList)
            if (parsedIH.getRoot() != infoHash)
                throw new InvalidSearchControlsException("infohash root doesn't match")
            
             return new UIResultEvent( sender : p,
                 name : name,
                 size : size,
                 infohash : parsedIH,
                 pieceSize : pieceSize,
                 uuid : uuid)
        } catch (Exception e) {
            throw new InvalidSearchResultException("parsing search result failed",e)
        }
    }
}
