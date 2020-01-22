package com.muwire.core.files

import com.muwire.core.Event
import com.muwire.core.SharedFile

class FileLoadedEvent extends Event {

    SharedFile loadedFile
    String source
}
