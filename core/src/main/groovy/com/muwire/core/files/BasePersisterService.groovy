package com.muwire.core.files

import com.muwire.core.DownloadedFile
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.Service
import com.muwire.core.SharedFile
import com.muwire.core.util.DataUtil
import net.i2p.data.Base64
import net.i2p.data.Destination

import java.util.stream.Collectors

abstract class BasePersisterService extends Service{

    protected static FileLoadedEvent fromJson(def json) {
        if (json.file == null || json.length == null || json.infoHash == null || json.hashList == null)
            throw new IllegalArgumentException()
        if (!(json.hashList instanceof List))
            throw new IllegalArgumentException()

        def file = new File(DataUtil.readi18nString(Base64.decode(json.file)))
        file = file.getCanonicalFile()
        if (!file.exists() || file.isDirectory())
            return null
        long length = Long.valueOf(json.length)
        if (length != file.length())
            return null

        List hashList = (List) json.hashList
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        hashList.each {
            byte [] hash = Base64.decode it.toString()
            if (hash == null)
                throw new IllegalArgumentException()
            baos.write hash
        }
        byte[] hashListBytes = baos.toByteArray()

        InfoHash ih = InfoHash.fromHashList(hashListBytes)
        byte [] root = Base64.decode(json.infoHash.toString())
        if (root == null)
            throw new IllegalArgumentException()
        if (!Arrays.equals(root, ih.getRoot()))
            return null

        int pieceSize = 0
        if (json.pieceSize != null)
            pieceSize = json.pieceSize
            
        if (json.sources != null) {
            List sources = (List)json.sources
            Set<Destination> sourceSet = sources.stream().map({ d -> new Destination(d.toString())}).collect Collectors.toSet()
            DownloadedFile df = new DownloadedFile(file, ih.getRoot(), pieceSize, sourceSet)
            df.setComment(json.comment)
            return new FileLoadedEvent(loadedFile : df, infoHash: ih)
        }


        SharedFile sf = new SharedFile(file, ih.getRoot(), pieceSize)
        sf.setComment(json.comment)
        if (json.downloaders != null)
            sf.getDownloaders().addAll(json.downloaders)
        if (json.searchers != null) {
            json.searchers.each {
                Persona searcher = null
                if (it.searcher != null)
                    searcher = new Persona(new ByteArrayInputStream(Base64.decode(it.searcher)))
                long timestamp = it.timestamp
                String query = it.query
                sf.hit(searcher, timestamp, query)
            }
        }
        return new FileLoadedEvent(loadedFile: sf, infoHash: ih)

    }
    
    protected static FileLoadedEvent fromJsonLite(json) {
        if (json.file == null || json.length == null || json.root == null)
            throw new IllegalArgumentException()
            
        def file = new File(DataUtil.readi18nString(Base64.decode(json.file)))
        file = file.getCanonicalFile()
        if (!file.exists() || file.isDirectory() || file.length() == 0 || !file.canRead())
            return null
        long length = Long.valueOf(json.length)
        if (length != file.length())
            return null
            
        byte[] root = Base64.decode(json.root)
        InfoHash ih = new InfoHash(root)
        
        int pieceSize = 0
        if (json.pieceSize != null)
            pieceSize = json.pieceSize

        boolean published = false
        long publishedTimestamp = -1
        if (json.published != null && json.published) {
            published = true
            publishedTimestamp = json.publishedTimestamp
        }

        if (json.sources != null) {
            List sources = (List)json.sources
            Set<Destination> sourceSet = sources.stream().map({ d -> new Destination(d.toString())}).collect Collectors.toSet()
            DownloadedFile df = new DownloadedFile(file, ih.getRoot(), pieceSize, sourceSet)
            if (published)
                df.publish(publishedTimestamp)
            df.setComment(json.comment)
            return new FileLoadedEvent(loadedFile : df, infoHash: ih)
        }


        SharedFile sf = new SharedFile(file, ih.getRoot(), pieceSize)
        sf.setComment(json.comment)
        if (published)
            sf.publish(publishedTimestamp)
        if (json.downloaders != null)
            sf.getDownloaders().addAll(json.downloaders)
        if (json.searchers != null) {
            json.searchers.each {
                Persona searcher = null
                if (it.searcher != null) {
                    try {
                        searcher = new Persona(new ByteArrayInputStream(Base64.decode(it.searcher)))
                    } catch (Exception ignore) {
                        return 
                    }
                }
                long timestamp = it.timestamp
                String query = it.query
                sf.hit(searcher, timestamp, query)
            }
        }
        return new FileLoadedEvent(loadedFile: sf, infoHash: ih)
    }

    protected static toJson(SharedFile sf) {
        def json = [:]
        json.file = sf.getB64EncodedFileName()
        json.length = sf.getCachedLength()
        json.root = Base64.encode(sf.getRoot())
        json.pieceSize = sf.getPieceSize()
        json.comment = sf.getComment()
        json.hits = sf.getHits()
        json.downloaders = sf.getDownloaders()

        if (!sf.searches.isEmpty()) {
            Set searchers = new HashSet<>()
            sf.searches.each {
                def search = [:]
                if (it.searcher != null)
                    search.searcher = it.searcher.toBase64()
                search.timestamp = it.timestamp
                search.query = it.query
                searchers.add(search)
            }
            json.searchers = searchers
        }

        if (sf instanceof DownloadedFile) {
            json.sources = sf.sources.stream().map( {d -> d.toBase64()}).collect(Collectors.toList())
        }
        
        if (sf.isPublished()) {
            json.published = true
            json.publishedTimestamp = sf.getPublishedTimestamp()
        }

        json
    }
}
