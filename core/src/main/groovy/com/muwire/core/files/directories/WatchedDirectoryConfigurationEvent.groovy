package com.muwire.core.files.directories

import com.muwire.core.Event
import com.muwire.core.Persona

class WatchedDirectoryConfigurationEvent extends Event {
    /** directory selected by user or by converter */
    File directory
    
    /** actual directories to apply, enriched by FileManager */
    File [] toApply
    
    boolean autoWatch
    int syncInterval
    boolean subfolders
    Visibility visibility
    Set<Persona> customVisibility
}
