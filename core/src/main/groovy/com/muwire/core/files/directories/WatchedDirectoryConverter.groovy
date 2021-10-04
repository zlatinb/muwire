package com.muwire.core.files.directories

import com.muwire.core.Core
import com.muwire.core.files.AllFilesLoadedEvent

/**
 * converts the setting-based format to new folder-based format.
 */
class WatchedDirectoryConverter {
    
    private final Core core
    
    WatchedDirectoryConverter(Core core) {
        this.core = core
    }
    
    void convert() {
        core.getMuOptions().getWatchedDirectories().each {
            File directory = new File(it)
            directory = directory.getCanonicalFile() 
            core.eventBus.publish(new WatchedDirectoryConfigurationEvent(directory : directory, autoWatch: true))
        }
        core.getMuOptions().getWatchedDirectories().clear()
        core.saveMuSettings()
        core.eventBus.publish(new WatchedDirectoryConvertedEvent())
    }
}
