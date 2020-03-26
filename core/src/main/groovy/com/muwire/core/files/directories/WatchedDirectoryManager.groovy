package com.muwire.core.files.directories

import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

import com.muwire.core.EventBus
import com.muwire.core.files.DirectoryWatchedEvent
import com.muwire.core.files.FileManager

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class WatchedDirectoryManager {
    
    private final File home
    private final EventBus eventBus
    private final FileManager fileManager
    
    private final Map<File, WatchedDirectory> watchedDirs = new ConcurrentHashMap<>()
    
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor({r -> 
        Thread t = new Thread(r, "disk-io")
        t.setDaemon(true)
        t
    } as ThreadFactory)
    
    private final Timer timer = new Timer("directory-timer", true)
    
    private boolean converting = true
    
    WatchedDirectoryManager(File home, EventBus eventBus, FileManager fileManager) {
        this.home = new File(home, "directories")
        this.home.mkdir()
        this.eventBus = eventBus
        this.fileManager = fileManager
    }
    
    public boolean isWatched(File f) {
        watchedDirs.containsKey(f)
    }
    
    public void shutdown() {
        diskIO.shutdown()
        timer.cancel()
    }
    
    void onWatchedDirectoryConfigurationEvent(WatchedDirectoryConfigurationEvent e) {
        if (converting) {
            def newDir = new WatchedDirectory(e.directory)
            // conversion is always autowatch really
            newDir.autoWatch = e.autoWatch
            persist(newDir)
        } else {
            // TODO: update state and stuff
        }
    }
    
    void onWatchedDirectoryConvertedEvent(WatchedDirectoryConvertedEvent e) {
        converting = false
        diskIO.submit({
            def slurper = new JsonSlurper()
            Files.walk(home.toPath()).filter({
                it.getFileName().toString().endsWith(".json")
            }).
            forEach {
                def parsed = slurper.parse(it.toFile())
                WatchedDirectory wd = WatchedDirectory.fromJson(parsed)
                watchedDirs.put(wd.directory, wd)
                
                if (wd.autoWatch)
                    eventBus.publish(new DirectoryWatchedEvent(directory : wd.directory))
            }
        } as Runnable)
    }
    
    private void persist(WatchedDirectory dir) {
        diskIO.submit({
            def json = JsonOutput.toJson(dir.toJson())
            def targetFile = new File(home, dir.getEncodedName() + ".json")
            targetFile.text = json
        } as Runnable)
    }
}
