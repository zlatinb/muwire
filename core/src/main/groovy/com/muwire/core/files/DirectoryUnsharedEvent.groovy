package com.muwire.core.files

import com.muwire.core.Event

class DirectoryUnsharedEvent extends Event {
    File directory
    
    public String toString() {
        super.toString() + " unshared directory "+ directory.toString()
    }
}
