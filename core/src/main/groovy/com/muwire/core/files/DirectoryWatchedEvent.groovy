package com.muwire.core.files

import com.muwire.core.Event

class DirectoryWatchedEvent extends Event {
    File directory
    boolean watch
    
    String toString() {
        "DirectoryWatchedEvent ${super.toString()} directory:$directory watch:$watch"
    }
}
