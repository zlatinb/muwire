package com.muwire.core.collections

import com.muwire.core.Event
import com.muwire.core.InfoHash
import com.muwire.core.Persona

class UIDownloadCollectionEvent extends Event {

    FileCollection collection
    InfoHash infoHash
    Set<FileCollectionItem> items
    Persona host
    boolean sequential
    boolean full
}
