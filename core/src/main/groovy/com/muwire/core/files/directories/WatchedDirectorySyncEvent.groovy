package com.muwire.core.files.directories

import com.muwire.core.Event

class WatchedDirectorySyncEvent extends Event {
    File directory
    long when
}
