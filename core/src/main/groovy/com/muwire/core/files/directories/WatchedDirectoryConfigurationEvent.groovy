package com.muwire.core.files.directories

import com.muwire.core.Event

class WatchedDirectoryConfigurationEvent extends Event {
    File directory
    boolean autoWatch
    int syncInterval
}
