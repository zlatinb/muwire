package com.muwire.core.files

import com.muwire.core.Event
import com.muwire.core.SharedFile

class FileUnsharedEvent extends Event {
    SharedFile[] unsharedFiles
    boolean deleted
    /**
     * true if the files are implicitly removed as part of unsharing a folder.
     */
    boolean implicit
}
