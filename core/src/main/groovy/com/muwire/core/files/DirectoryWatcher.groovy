package com.muwire.core.files

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService

import com.muwire.core.EventBus

import groovy.util.logging.Log

@Log
class DirectoryWatcher {
    
    private final EventBus eventBus
    private final Thread watcherThread
    private WatchService watchService
    
    DirectoryWatcher(EventBus eventBus) {
        this.eventBus = eventBus
        this.watcherThread = new Thread({watch() } as Runnable, "directory-watcher")
    }
    
    void start() {
        watchService = FileSystems.getDefault().newWatchService()
        watcherThread.start()
    }
    
    void stop() {
        watcherThread.interrupt()
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
        while(true) {
            WatchKey key = watchService.take()
            key.pollEvents().each {
                if (it.kind() == StandardWatchEventKinds.ENTRY_MODIFY)
                    processModified(key.watchable(), it.context()) 
            }
            key.reset()
        }
    }
    
    
    private void processModified(Path parent, Path path) {
        File f = join(parent, path)
        eventBus.publish(new FileSharedEvent(file : f))
    }
    
    private static File join(Path parent, Path path) {
        File parentFile = parent.toFile().getCanonicalFile()
        new File(parentFile, path.toFile().getName())
    }
}
