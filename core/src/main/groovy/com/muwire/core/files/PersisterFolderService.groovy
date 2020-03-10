package com.muwire.core.files

import com.muwire.core.*
import com.muwire.core.filefeeds.UIFilePublishedEvent
import com.muwire.core.filefeeds.UIFileUnpublishedEvent

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.logging.Level

/**
 * A persister that stores information about the files shared using
 * individual JSON files in directories.
 *
 * The absolute path's 32bit hash to the shared file is used
 * to build the directory and filename.
 *
 * This persister only starts working once the old persister has finished loading
 * @see PersisterFolderService#getJsonPath
 */
@Log
class PersisterFolderService extends BasePersisterService {

    final static int CUT_LENGTH = 6

    private final Core core;
    final File location
    final EventBus listener
    final int interval
    final Timer timer
    final ExecutorService persisterExecutor = Executors.newSingleThreadExecutor({ r ->
        new Thread(r, "file persister")
    } as ThreadFactory)

    PersisterFolderService(Core core, File location, EventBus listener) {
        this.core = core;
        this.location = location
        this.listener = listener
        this.interval = interval
        timer = new Timer("file-folder persister timer", true)
    }

    void stop() {
        timer.cancel()
        persisterExecutor.shutdown()
    }

    void onPersisterDoneEvent(PersisterDoneEvent persisterDoneEvent) {
        log.info("Old persister done")
        load()
    }

    void onFileHashedEvent(FileHashedEvent hashedEvent) {
        persistFile(hashedEvent.sharedFile, hashedEvent.infoHash)
    }

    void onFileDownloadedEvent(FileDownloadedEvent downloadedEvent) {
        if (core.getMuOptions().getShareDownloadedFiles()) {
            persistFile(downloadedEvent.downloadedFile, downloadedEvent.infoHash)
        }
    }

    /**
     * Get rid of the json and hashlists of unshared files
     * @param unsharedEvent
     */
    void onFileUnsharedEvent(FileUnsharedEvent unsharedEvent) {
        def jsonPath = getJsonPath(unsharedEvent.unsharedFile)
        def jsonFile = jsonPath.toFile()
        if(jsonFile.isFile()){
            jsonFile.delete()
        }
        def hashListPath = getHashListPath(unsharedEvent.unsharedFile)
        def hashListFile = hashListPath.toFile()
        if (hashListFile.isFile())
            hashListFile.delete()
    }
    
    void onFileLoadedEvent(FileLoadedEvent loadedEvent) {
        if(loadedEvent.source == "PersisterService"){
            log.info("Migrating persisted file from PersisterService: "
                    + loadedEvent.loadedFile.file.absolutePath.toString())
            persistFile(loadedEvent.loadedFile, loadedEvent.infoHash)
        }
    }
    
    void onUICommentEvent(UICommentEvent e) {
        persistFile(e.sharedFile,null)
    }
    
    void onUIFilePublishedEvent(UIFilePublishedEvent e) {
        persistFile(e.sf, null)
    }
    
    void onUIFileUnpublishedEvent(UIFileUnpublishedEvent e) {
        persistFile(e.sf, null)
    }

    void load() {
        log.fine("Loading...")
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY)

        if (location.exists() && location.isDirectory()) {
            try {
                _load()
            }
            catch (IllegalArgumentException e) {
                log.log(Level.WARNING, "couldn't load files", e)
            }
        } else {
            location.mkdirs()
            listener.publish(new AllFilesLoadedEvent())
        }
        loaded = true
    }

    /**
     * Loads every JSON into memory
     */
    private void _load() {
        int loaded = 0
        def slurper = new JsonSlurper()
        Files.walk(location.toPath())
                .filter({
                    it.getFileName().toString().endsWith(".json")
                })
                .forEach({
                    def parsed = slurper.parse it.toFile()
                    def event = fromJsonLite parsed
                    if (event == null) return

                    log.fine("loaded file $event.loadedFile.file")
                    listener.publish event
                    loaded++
                    if (loaded % 10 == 0)
                        Thread.sleep(20)

                })
        listener.publish(new AllFilesLoadedEvent())
    }

    private void persistFile(SharedFile sf, InfoHash ih) {
        persisterExecutor.submit({
            def jsonPath = getJsonPath(sf)

            def startTime = System.currentTimeMillis()
            jsonPath.parent.toFile().mkdirs()
            jsonPath.toFile().withPrintWriter { writer ->
                def json = toJson sf
                json = JsonOutput.toJson(json)
                writer.println json
            }
            
            if (ih != null) {
                def hashListPath = getHashListPath(sf)
                hashListPath.toFile().bytes = ih.hashList
            }
            log.fine("Time(ms) to write json+hashList: " + (System.currentTimeMillis() - startTime))
        } as Runnable)
    }
    private Path getJsonPath(SharedFile sf){
        def pathHash = sf.getB64PathHash()
        return Paths.get(
                location.getAbsolutePath(),
                pathHash.substring(0, CUT_LENGTH),
                pathHash.substring(CUT_LENGTH) + ".json"
        )
    }
    
    private Path getHashListPath(SharedFile sf) {
        def pathHash = sf.getB64PathHash()
        return Paths.get(
                location.getAbsolutePath(),
                pathHash.substring(0, CUT_LENGTH),
                pathHash.substring(CUT_LENGTH) + ".hashlist"
        )
    }
    
    InfoHash loadInfoHash(SharedFile sf) {
        def path = getHashListPath(sf)
        InfoHash.fromHashList(path.toFile().bytes)
    }
}
