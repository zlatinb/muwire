package com.muwire.webui;

import java.io.File;

import com.muwire.core.Core;
import com.muwire.core.files.DirectoryUnsharedEvent;
import com.muwire.core.files.DirectoryWatchedEvent;
import com.muwire.core.files.directories.UISyncDirectoryEvent;
import com.muwire.core.files.directories.WatchedDirectoryConfigurationEvent;
import com.muwire.core.files.directories.WatchedDirectorySyncEvent;

public class AdvancedSharingManager {
    
    private final Core core;
    private volatile long revision;
    
    public AdvancedSharingManager(Core core) {
        this.core = core;
    }
    
    public long getRevision() {
        return revision;
    }
    
    public void onDirectoryWatchedEvent(DirectoryWatchedEvent e) {
        revision++;
    }
    
    public void onDirectoryUnsharedEvent(DirectoryUnsharedEvent e) {
        revision++;
    }
    
    public void onWatchedDirectorySyncEvent(WatchedDirectorySyncEvent e) {
        revision++;
    }
    

    void sync(File dir) {
        revision++;
        UISyncDirectoryEvent event = new UISyncDirectoryEvent();
        event.setDirectory(dir);
        core.getEventBus().publish(event);
    }

    void configure(File dir, boolean autoWatch, int syncInterval) {
        revision++;
        WatchedDirectoryConfigurationEvent event = new WatchedDirectoryConfigurationEvent();
        event.setAutoWatch(autoWatch);
        event.setDirectory(dir);
        event.setSyncInterval(syncInterval);
        core.getEventBus().publish(event);
    }
}
