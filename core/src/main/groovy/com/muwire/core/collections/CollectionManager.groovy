package com.muwire.core.collections

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.logging.Level

import com.muwire.core.EventBus
import com.muwire.core.SharedFile
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.files.AllFilesLoadedEvent
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileManager
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.search.ResultsEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.search.SearchIndex

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import net.i2p.data.Base64

@CompileStatic
@Log
class CollectionManager {
    
    private final EventBus eventBus
    private final FileManager fileManager

    private final File localCollections
    private final File remoteCollections
    
    /** infohash of the collection to collection */
    private final Map<InfoHash, FileCollection> rootToCollection = new HashMap<>()
    /** infohash of a collection item to every collection it is part of */
    private final Map<InfoHash, Set<FileCollection>> fileRootToCollections = new HashMap<>()
    /** FileCollection object to it's corresponding infohash */
    private final Map<FileCollection, InfoHash> collectionToHash = new HashMap<>()
    /** infohash of the collection to collection */
    private final Map<InfoHash, FileCollection> rootToCollectionRemote = new HashMap<>()
    private final Map<FileCollection, Set<InfoHash>> filesInRemoteCollection = new HashMap<>()
    
    private final SearchIndex index = new SearchIndex()
    private final Map<String, Set<FileCollection>> nameToCollection = new HashMap<>()
    private final Map<String, Set<FileCollection>> commentToCollection = new HashMap<>()
    
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor({ Runnable r ->
        new Thread(r, "collections-io")
    } as ThreadFactory)
    
    public CollectionManager(EventBus eventBus, FileManager fileManager, File home) {
        this.eventBus = eventBus
        this.fileManager = fileManager

        File collections = new File(home, "collections")
        localCollections = new File(collections, "local")
        remoteCollections = new File(collections, "remote")
        
        localCollections.mkdirs()
        remoteCollections.mkdirs()        
    }

    synchronized List<FileCollection> getCollections() {
        new ArrayList<>(rootToCollection.values())
    }
    
    synchronized FileCollection getByInfoHash(InfoHash ih) {
        rootToCollection.get(ih)
    }
    
    synchronized Set<InfoHash> collectionsForFile(InfoHash ih) {
        Set<InfoHash> rv = Collections.emptySet()
        if (fileRootToCollections.containsKey(ih)) {
            rv = new HashSet<>()
            fileRootToCollections.get(ih).collect(rv, { collectionToHash.get(it) })
        }
        rv
    }
        
    void onAllFilesLoadedEvent(AllFilesLoadedEvent e) {
        diskIO.execute({load()} as Runnable)
    }
    
    void stop() {
        diskIO.shutdown()
    }
    
    private void load() {
        log.info("loading collections")
        Files.walk(localCollections.toPath())
            .filter({it.getFileName().toString().endsWith(".mwcollection")})
            .forEach({ path ->
                log.fine("processing $path")
                try {
                    File f = path.toFile()
                    FileCollection collection
                    f.withInputStream { 
                        collection = new FileCollection(it)
                    }
                    boolean allFilesShared = true
                    collection.files.each {
                        allFilesShared &= fileManager.isShared(it.infoHash)
                    }
                    if (allFilesShared) {
                        addToIndex(collection.getInfoHash(), collection)
                        eventBus.publish(new CollectionLoadedEvent(collection : collection, local : true))
                    } else {
                        log.fine("not all files were shared from collection $path, deleting")
                        f.delete()
                    }       
                } catch (Exception e) {
                    log.log(Level.WARNING, "failed to load collection $path", e)
                }
            })
            
        Files.walk(remoteCollections.toPath())
                .filter({it.getFileName().toString().endsWith(".mwcollection")})
                .forEach { path ->
                    log.fine("processing $path")
                    try {
                        File f = path.toFile()
                        FileCollection collection
                        f.withInputStream {
                            collection = new FileCollection(it)
                        }
                        Set<InfoHash> remaining = new HashSet<>()
                        collection.files.each {
                            if (!fileManager.isShared(it.infoHash))
                                remaining.add(it.infoHash)
                        }
                        InfoHash infoHash = collection.getInfoHash()
                        if (!remaining.isEmpty()) {
                            synchronized(this) {
                                filesInRemoteCollection.put(collection, remaining)
                                rootToCollectionRemote.put(infoHash, collection)
                            }
                        } else {
                            log.fine("all files of remote collection were shared, moving to local")
                            File target = new File(localCollections, f.getName())
                            Files.move(path, target.toPath(), StandardCopyOption.ATOMIC_MOVE)
                            addToIndex(infoHash, collection)
                        }
                    } catch (Exception e) {
                        log.log(Level.WARNING, "failed to load collection $path", e)
                    }
                }
    }
    
    void onUICollectionCreatedEvent(UICollectionCreatedEvent e) {
        diskIO.execute({
            persist(e.collection, localCollections)
            addToIndex(e.collection.getInfoHash(), e.collection)
            } as Runnable)
    }
    
    private void persist(FileCollection collection, File parent) {
        
        InfoHash infoHash = collection.getInfoHash()
        String hashB64 = Base64.encode(infoHash.getRoot())
        String fileName = "${hashB64}_${collection.author.getHumanReadableName()}_${collection.timestamp}.mwcollection"
        
        File file = new File(parent, fileName)
        file.bytes = collection.getPayload()
        
        log.info("persisted ${fileName}")
    }
    
    private synchronized void addToIndex(InfoHash infoHash, FileCollection collection) {
        rootToCollection.put(infoHash, collection)
        collectionToHash.put(collection, infoHash)
        collection.files.each { 
            Set<FileCollection> set = fileRootToCollections.get(it.infoHash)
            if (set == null) {
                set = new HashSet<>()
                fileRootToCollections.put(it.infoHash, set)
            }
            set.add(collection)
        }
        
        index.add(collection.name)
        Set<FileCollection> existing = nameToCollection.get(collection.name)
        if (existing == null) {
            existing = new HashSet<>()
            nameToCollection.put(collection.name, existing)
        }
        existing.add(collection)
        
        if (collection.comment != "") {
            index.add(collection.comment)
            existing = commentToCollection.get(collection.comment)
            if (existing == null) {
                existing = new HashSet<>()
                commentToCollection.put(collection.comment, existing)
            }
            existing.add(collection)
        }
    }
    
    void onUICollectionDeletedEvent(UICollectionDeletedEvent e) {
        diskIO.execute({delete(e.collection)} as Runnable)
    }
    
    private void delete(FileCollection collection) {
        InfoHash infoHash = collection.getInfoHash()
        String hashB64 = Base64.encode(infoHash.getRoot())
        String fileName = "${hashB64}_${collection.author.getHumanReadableName()}_${collection.timestamp}.mwcollection"

        log.fine("deleting $fileName")        
        File file = new File(localCollections, fileName)
        file.delete()
        
        removeFromIndex(infoHash, collection)
    }
    
    private synchronized void removeFromIndex(InfoHash infoHash, FileCollection collection) {
        rootToCollection.remove(infoHash)
        collectionToHash.remove(collection)
        collection.files.each { 
            Set<FileCollection> set = fileRootToCollections.get(it.infoHash)
            if (set == null)
                return // ?
            set.remove(collection)
            if (set.isEmpty())
                fileRootToCollections.remove(it.infoHash)
        }
        
        index.remove(collection.name)
        Set<FileCollection> existing = nameToCollection.get(collection.name)
        if (existing != null) {
            existing.remove(collection)
            if (existing.isEmpty())
                nameToCollection.remove(collection.name)
        }
        
        if (collection.comment != "") {
            index.remove(collection.comment)
            existing = commentToCollection.get(collection.comment)
            if (existing != null) {
                existing.remove(collection)
                if (existing.isEmpty())
                    commentToCollection.remove(collection.comment)
            }
        }
    }
    
    synchronized void onUIDownloadCollectionEvent(UIDownloadCollectionEvent e) {
        if (!e.full)
            return
        rootToCollectionRemote.put(e.infoHash, e.collection)
        Set<InfoHash> infoHashes = new HashSet<>()
        e.collection.files.collect(infoHashes, {it.infoHash})
        filesInRemoteCollection.put(e.collection, infoHashes)
        diskIO.execute({persist(e.collection, remoteCollections)} as Runnable)
    }
    
    synchronized void onFileDownloadedEvent(FileDownloadedEvent e) {
        FileCollection collection = rootToCollectionRemote.get(e.collectionInfoHash)
        if (collection == null)
            return
        Set<InfoHash> infoHashes = filesInRemoteCollection.get(collection)
        if (infoHashes == null)
            return
        infoHashes.remove(e.infoHash)
        if (infoHashes.isEmpty()) {

            String hashB64 = Base64.encode(e.collectionInfoHash.getRoot())            
            String fileName = "${hashB64}_${collection.author.getHumanReadableName()}_${collection.timestamp}.mwcollection"
            log.info("moving $fileName to local collections")

            File file = new File(remoteCollections, fileName)
            File target = new File(localCollections, fileName)
            Files.move(file.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
            addToIndex(e.collectionInfoHash, collection)
        }
    }
    
    synchronized void onFileUnsharedEvent(FileUnsharedEvent e) {
        InfoHash infoHash = new InfoHash(e.unsharedFile.getRoot())
        Set<FileCollection> affected = fileRootToCollections.get(infoHash)
        if (affected == null || affected.isEmpty()) {
            return
        }
        affected.each { c ->
            diskIO.execute({delete(c)} as Runnable)
        }
    }
    
    synchronized void onSearchEvent(SearchEvent e) {
        if (!e.collections)
            return
        
        if (e.searchHash != null) {   
            InfoHash ih = new InfoHash(e.searchHash)
            def collection = rootToCollection.get(ih)
            if (collection == null)
                return
            collection.hit(e.persona)

            List<SharedFile> sharedFiles = new ArrayList<>()
            collection.files.each { item ->
                def sfs = fileManager.getRootToFiles().get(item.infoHash)
                if (sfs == null || sfs.isEmpty())
                    return // hmm
                sfs.each { sf -> sf.hit(e.persona, e.timestamp, "Collection Search")}
                sharedFiles.addAll(sfs)
            }
            def resultEvent = new ResultsEvent(results : sharedFiles.toArray(new SharedFile[0]), uuid : e.uuid, searchEvent : e)
            eventBus.publish(resultEvent)
        } else {
            String[]names = index.search(e.searchTerms)
            Set<FileCollection> collections = new HashSet<>()
            for(String name : names) {
                Set<FileCollection> match = nameToCollection.get(name)
                if (match != null)
                    collections.addAll(match)
                if (e.searchComments) {
                    match = commentToCollection.get(name)
                    if (match != null)
                        collections.addAll(match)
                }
            }
            
            if (collections.isEmpty())
                return
                
            List<SharedFile> sharedFiles = new ArrayList<>()
            collections.each { c ->
                c.hit(e.persona)
                c.files.each { f-> 
                    def sfs = fileManager.getRootToFiles().get(f.infoHash)
                    if (sfs == null || sfs.isEmpty())
                        return
                    sfs.each { sf -> sf.hit(e.persona, e.timestamp, String.join(" ", e.searchTerms))}
                    sharedFiles.addAll(sfs)
                }
            }
            if (sharedFiles.isEmpty())
                return
            def resultEvent = new ResultsEvent(results : sharedFiles.toArray(new SharedFile[0]), uuid : e.uuid, searchEvent : e)
            eventBus.publish(resultEvent)
        }
    }
}
