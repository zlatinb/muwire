package com.muwire.core.collections

import com.muwire.core.Event
import com.muwire.core.Persona

class UIDownloadCollectionEvent extends Event {

    FileCollection collection
    Set<FileCollectionItem> items
    boolean full
    Persona host
}
