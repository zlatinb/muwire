package com.muwire.core.files.directories

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

import com.muwire.core.EventBus
import com.muwire.core.files.FileManager

class WatchedDirectoryManager {
    
    private final File home
    private final EventBus eventBus
    private final FileManager fileManager
    
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor({r -> 
        Thread t = new Thread(r, "disk-io")
        t.setDaemon(true)
        t
    } as ThreadFactory)
    
    private final Timer timer = new Timer("directory-timer", true)
    
    private boolean converting = true
    
    WatchedDirectoryManager(File home, EventBus eventBus, FileManager fileManager) {
        this.home = new File(home, "directories")
        this.eventBus = eventBus
        this.fileManager = fileManager
    }
    
    public void shutdown() {
        diskIO.shutdown()
        timer.cancel()
    }
    
    void onWatchedDirectoryConfigurationEvent(WatchedDirectoryConfigurationEvent e) {
        if (!converting) {
            // update state
        }
        
        // always persist
    }
    
    void onWatchedDirectoryConvertedEvent(WatchedDirectoryConvertedEvent e) {
        converting = false
        // load
    }
}
