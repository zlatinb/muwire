package com.muwire.core.files

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import static java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.EventBus
import com.muwire.core.SharedFile

import groovy.util.logging.Log
import net.i2p.util.SystemVersion

@Log
class DirectoryWatcher {
    
    private static final long WAIT_TIME = 1000
    
    private static final WatchEvent.Kind[] kinds
    static {
        if (SystemVersion.isMac())
            kinds = [ENTRY_MODIFY, ENTRY_DELETE]
        else
            kinds = [ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE]
    }
    
    private final EventBus eventBus
    private final FileManager fileManager
    private final Thread watcherThread, publisherThread
    private final Map<File, Long> waitingFiles = new ConcurrentHashMap<>()
    private WatchService watchService
    private volatile boolean shutdown
    
    DirectoryWatcher(EventBus eventBus, FileManager fileManager) {
        this.eventBus = eventBus
        this.fileManager = fileManager
        this.watcherThread = new Thread({watch() } as Runnable, "directory-watcher")
        watcherThread.setDaemon(true)
        this.publisherThread = new Thread({publish()} as Runnable, "watched-files-publisher")
        publisherThread.setDaemon(true)
    }
    
    void start() {
        watchService = FileSystems.getDefault().newWatchService()
        watcherThread.start()
        publisherThread.start()
    }
    
    void stop() {
        shutdown = true
        watcherThread.interrupt()
        publisherThread.interrupt()
        watchService.close()
    }
    
    void onFileSharedEvent(FileSharedEvent e) {
        if (!e.file.isDirectory())
            return
        Path path = e.file.getCanonicalFile().toPath()
        path.register(watchService, kinds)
        
    }

    private void watch() {
        try {
            while(!shutdown) {
                WatchKey key = watchService.take()
                key.pollEvents().each {
                    switch(it.kind()) {
                        case ENTRY_CREATE: processCreated(key.watchable(), it.context()); break
                        case ENTRY_MODIFY: processModified(key.watchable(), it.context()); break
                        case ENTRY_DELETE: processDeleted(key.watchable(), it.context()); break
                    }
                }
                key.reset()
            }
        } catch (InterruptedException e) {
            if (!shutdown)
                throw e
        }
    }
    

    private void processCreated(Path parent, Path path) {
        File f= join(parent, path)
        log.fine("created entry $f")
        if (f.isDirectory())
            f.toPath().register(watchService, kinds)
    }
        
    private void processModified(Path parent, Path path) {
        File f = join(parent, path)
        log.fine("modified entry $f")
        waitingFiles.put(f, System.currentTimeMillis())
    }
    
    private void processDeleted(Path parent, Path path) {
        File f = join(parent, path)
        log.fine("deleted entry $f")
        SharedFile sf = fileManager.fileToSharedFile.get(f)
        if (sf != null)
            eventBus.publish(new FileUnsharedEvent(unsharedFile : sf))
    }
    
    private static File join(Path parent, Path path) {
        File parentFile = parent.toFile().getCanonicalFile()
        new File(parentFile, path.toFile().getName())
    }
    
    private void publish() {
        try {
            while(!shutdown) {
                Thread.sleep(WAIT_TIME)
                long now = System.currentTimeMillis()
                def published = []
                waitingFiles.each { file, timestamp ->
                    if (now - timestamp > WAIT_TIME) {
                        log.fine("publishing file $file")
                        eventBus.publish new FileSharedEvent(file : file)
                        published << file
                    }
                }
                published.each {
                    waitingFiles.remove(it)
                }
            }
        } catch (InterruptedException e) {
            if (!shutdown)
                throw e
        }
    }
}
