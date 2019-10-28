package com.muwire.core.search

import java.util.stream.Collectors

import javax.naming.directory.InvalidSearchControlsException

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.util.DataUtil

import net.i2p.data.Base64
import net.i2p.data.Destination

class ResultsParser {
    public static UIResultEvent parse(Persona p, UUID uuid, def json) throws InvalidSearchResultException {
        if (json.type != "Result")
            throw new InvalidSearchResultException("not a result json")
        switch(json.version) {
            case 1:
                return parseV1(p, uuid, json)
            case 2:
                return parseV2(p, uuid, json)
            default:
                throw new InvalidSearchResultException("unknown version $json.version")

        }

    }

    private static parseV1(Persona p, UUID uuid, def json) {
        if (json.name == null)
            throw new InvalidSearchResultException("name missing")
        if (json.size == null || json.size == 0)
            throw new InvalidSearchResultException("length missing")
        if (json.infohash == null)
            throw new InvalidSearchResultException("infohash missing")
        if (json.pieceSize == null || json.pieceSize <= 0)
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
                 sources : Collections.emptySet(),
                 uuid : uuid)
        } catch (Exception e) {
            throw new InvalidSearchResultException("parsing search result failed",e)
        }
    }

    private static UIResultEvent parseV2(Persona p, UUID uuid, def json) {
        if (json.name == null)
            throw new InvalidSearchResultException("name missing")
        if (json.size == null || json.size == 0)
            throw new InvalidSearchResultException("length missing")
        if (json.infohash == null)
            throw new InvalidSearchResultException("infohash missing")
        if (json.pieceSize == null || json.pieceSize <= 0)
            throw new InvalidSearchResultException("pieceSize missing")
        if (json.hashList != null)
            throw new InvalidSearchResultException("V2 result with hashlist")
        try {
            String name = DataUtil.readi18nString(Base64.decode(json.name))
            long size = json.size
            byte [] infoHash = Base64.decode(json.infohash)
            if (infoHash.length != InfoHash.SIZE)
                throw new InvalidSearchResultException("invalid infohash size $infoHash.length")
            int pieceSize = json.pieceSize

            Set<Destination> sources = Collections.emptySet()
            if (json.sources != null)
                sources = json.sources.stream().map({new Destination(it)}).collect(Collectors.toSet())
                
            String comment = null
            if (json.comment != null)
                comment = DataUtil.readi18nString(Base64.decode(json.comment))
                
            boolean browse = false
            if (json.browse != null)
                browse = json.browse

            return new UIResultEvent( sender : p,
                name : name,
                size : size,
                infohash : new InfoHash(infoHash),
                pieceSize : pieceSize,
                sources : sources,
                comment : comment,
                browse : browse,
                uuid: uuid)
        } catch (Exception e) {
            throw new InvalidSearchResultException("parsing search result failed",e)
        }
    }
}
