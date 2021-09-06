package com.muwire.core.files

import com.muwire.core.Event

class DirectoryUnsharedEvent extends Event {
    File[] directories
    boolean deleted
    
    public String toString() {
        super.toString() + " unshared directories "+ Arrays.toString(directories) + " deleted $deleted"
    }
}
