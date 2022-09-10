package com.muwire.core.files

import com.muwire.core.Event
import com.muwire.core.SharedFile

class FileModifiedEvent extends Event {
    SharedFile[] sharedFiles
}
