package com.muwire.core.files

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.EventBus

import groovy.util.logging.Log

@Log
class DirectoryWatcher {
    
    private static final long WAIT_TIME = 1000
    
    private final EventBus eventBus
    private final Thread watcherThread, publisherThread
    private final Map<File, Long> waitingFiles = new ConcurrentHashMap<>()
    private WatchService watchService
    private volatile boolean shutdown
    
    DirectoryWatcher(EventBus eventBus) {
        this.eventBus = eventBus
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
        path.register(watchService, 
            StandardWatchEventKinds.ENTRY_MODIFY, 
            StandardWatchEventKinds.ENTRY_DELETE)
        
    }

    private void watch() {
        try {
            while(!shutdown) {
                WatchKey key = watchService.take()
                key.pollEvents().each {
                    if (it.kind() == StandardWatchEventKinds.ENTRY_MODIFY)
                        processModified(key.watchable(), it.context())
                }
                key.reset()
            }
        } catch (InterruptedException e) {
            if (!shutdown)
                throw e
        }
    }
    
    
    private void processModified(Path parent, Path path) {
        File f = join(parent, path)
        waitingFiles.put(f, System.currentTimeMillis())
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
