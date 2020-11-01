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
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.files.AllFilesLoadedEvent
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileManager

import groovy.util.logging.Log
import net.i2p.data.Base64

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
    
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor({ r ->
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
        def rv = Collections.emptySet()
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
                        PayloadAndIH pih = infoHash(collection)
                        addToIndex(pih.infoHash, collection)
                        eventBus.publish(new CollectionLoadedEvent(collection : collection, local : true))
                    } else {
                        log.fine("not all files were shared from collection $path, deleting")
                        f.delete()
                    }       
                } catch (Exception e) {
                    log.log(Level.WARNING, "failed to load collection $path", e)
                }
            })
    }
    
    void onUICollectionCreatedEvent(UICollectionCreatedEvent e) {
        diskIO.execute({
            def pih = persist(e.collection, localCollections)
            addToIndex(pih.infoHash, e.collection)
            } as Runnable)
    }
    
    private PayloadAndIH persist(FileCollection collection, File parent) {
        
        PayloadAndIH pih = infoHash(collection)
        String hashB64 = Base64.encode(pih.infoHash.getRoot())
        String fileName = "${hashB64}_${collection.author.getHumanReadableName()}_${collection.timestamp}.mwcollection"
        
        File file = new File(parent, fileName)
        file.bytes = pih.payload
        
        log.info("persisted ${fileName}")
        pih
    }
    
    public static InfoHash hash(FileCollection collection) {
        return infoHash(collection).infoHash
    }
    
    private static PayloadAndIH infoHash(FileCollection collection) {
        def baos = new ByteArrayOutputStream()
        collection.write(baos)
        byte [] payload = baos.toByteArray()

        MessageDigest digester = MessageDigest.getInstance("SHA-256");
        digester.update(payload)
        InfoHash infoHash = new InfoHash(digester.digest())
        new PayloadAndIH(infoHash, payload)
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
    }
    
    private static class PayloadAndIH {
        private final InfoHash infoHash
        private final byte [] payload
        PayloadAndIH(InfoHash infoHash, byte[] payload) {
            this.infoHash = infoHash
            this.payload = payload
        }
    }
    
    void onUICollectionDeletedEvent(UICollectionDeletedEvent e) {
        diskIO.execute({delete(e.collection)} as Runnable)
    }
    
    private void delete(FileCollection collection) {
        PayloadAndIH pih = infoHash(collection)
        String hashB64 = Base64.encode(pih.infoHash.getRoot())
        String fileName = "${hashB64}_${collection.author.getHumanReadableName()}_${collection.timestamp}.mwcollection"
        
        File file = new File(localCollections, fileName)
        file.delete()
        
        removeFromIndex(pih.infoHash, collection)
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
    }
    
    synchronized void onUIDownloadCollectionEvent(UIDownloadCollectionEvent e) {
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
}
