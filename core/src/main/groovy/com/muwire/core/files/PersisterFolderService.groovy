package com.muwire.core.files

import com.muwire.core.*
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

    final File location
    final EventBus listener
    final int interval
    final Timer timer
    final ExecutorService persisterExecutor = Executors.newSingleThreadExecutor({ r ->
        new Thread(r, "file persister")
    } as ThreadFactory)

    PersisterFolderService(File location, EventBus listener) {
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
        load()
    }

    void onUIPersistFilesEvent(UIPersistFilesEvent e) {
        persistFiles()
    }

    void onFileHashedEvent(FileHashedEvent hashedEvent) {
        persistFile(hashedEvent.sharedFile)
    }
    /**
     * Get rid of the json of unshared files
     * @param unsharedEvent
     */
    void onFileUnsharedEvent(FileUnsharedEvent unsharedEvent) {
        def jsonPath = getJsonPath(unsharedEvent.unsharedFile)
        def jsonFile = jsonPath.toFile()
        if(jsonFile.isFile()){
            jsonFile.delete()
        }
    }
    void onFileLoadedEvent(FileLoadedEvent loadedEvent) {
        if(loadedEvent.sourceClass == PersisterService){
            log.info("Migrating persisted file from PersisterService")
            persistFile(loadedEvent.loadedFile)
        }
    }

    void load() {
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
     *
     * TODO: Decide if this is a good idea
     *       The more shared files, the more we'll load in memory.
     *       It might not be necessary
     */
    private void _load() {
        int loaded = 0
        def slurper = new JsonSlurper()
        Files.walk(location.toPath())
                .filter({ it.fileName.endsWith(".json") })
                .forEach({
                    def parsed = slurper.parse it.toFile()
                    def event = fromJson parsed
                    if (event == null) return

                    log.fine("loaded file $event.loadedFile.file")
                    listener.publish event
                    loaded++
                    if (loaded % 10 == 0)
                        Thread.sleep(20)

                })
        listener.publish(new AllFilesLoadedEvent())
    }

    private void persistFile(SharedFile sf) {
        persisterExecutor.submit({
            def jsonPath = getJsonPath(sf)

            def startTime = System.currentTimeMillis()
            jsonPath.parent.toFile().mkdir()
            jsonPath.toFile().withPrintWriter { writer ->
                def json = toJson sf
                json = JsonOutput.toJson(json)
                writer.println json
            }
            log.fine("Time(ms) to write json: " + (System.currentTimeMillis() - startTime))
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
}
