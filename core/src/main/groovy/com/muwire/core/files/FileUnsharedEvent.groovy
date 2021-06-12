package com.muwire.core.files

import com.muwire.core.Event
import com.muwire.core.SharedFile

class FileUnsharedEvent extends Event {
    SharedFile[] unsharedFiles
    boolean deleted
}
