package com.muwire.core.files

import com.muwire.core.*
import com.muwire.core.filefeeds.UIFilePublishedEvent
import com.muwire.core.filefeeds.UIFileUnpublishedEvent
import com.muwire.core.util.DataUtil
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.stream.Stream

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
        persisterExecutor.execute({load()} as Runnable)
    }

    void onFileHashedEvent(FileHashedEvent hashedEvent) {
        if (core.getMuOptions().getAutoPublishSharedFiles() && hashedEvent.sharedFile != null) 
            hashedEvent.sharedFile.publish(System.currentTimeMillis())
        persistFile(hashedEvent.sharedFile, hashedEvent.infoHash)
    }

    void onFileDownloadedEvent(FileDownloadedEvent downloadedEvent) {
        if (core.getMuOptions().getShareDownloadedFiles()) {
            if (core.getMuOptions().getAutoPublishSharedFiles())
                downloadedEvent.downloadedFile.publish(System.currentTimeMillis())
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
            catch (Exception e) {
                log.log(Level.WARNING, "couldn't load files", e)
            }
        } else {
            location.mkdirs()
            listener.publish(new AllFilesLoadedEvent())
        }
        loaded = true
    }

    /**
     * Loads every JSON into memory.  If this is the plugin, load right away.
     * If it's the standalone throttle and use a single thread because
     * rapid fire events can make the GUI unresponsive.
     */
    private void _load() {
        int loaded = 0
        AtomicInteger failed = new AtomicInteger()
        Stream<Path> stream = Files.walk(location.toPath())
        if (core.muOptions.plugin)
            stream = stream.parallel()
        stream.filter({
            it.getFileName().toString().endsWith(".json")
        })
        .forEach({
            log.fine("processing path $it")
            def slurper = new JsonSlurper()
            try {
                def parsed = slurper.parse it.toFile()
                def event = fromJsonLite parsed
                if (event == null) return

                if (core.muOptions.ignoredFileTypes.contains(DataUtil.getFileExtension(event.loadedFile.file))) {
                    log.fine("ignoring file ${event.loadedFile.file}")
                    return
                }
                
                log.fine("loaded file $event.loadedFile.file")
                listener.publish event
                if (!core.muOptions.plugin) {
                    loaded++
                    if (loaded % 10 == 0)
                        Thread.sleep(20)
                }
            } catch (Exception e) {
                log.log(Level.WARNING,"failed to load $it",e)
                failed.incrementAndGet()
            }
        })
        listener.publish(new AllFilesLoadedEvent(failed : failed.get()))
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
