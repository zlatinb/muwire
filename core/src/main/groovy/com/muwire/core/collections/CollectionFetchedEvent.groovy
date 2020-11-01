package com.muwire.core.collections

import com.muwire.core.Event
import com.muwire.core.InfoHash

class CollectionFetchedEvent extends Event {
    FileCollection collection
    UUID uuid
    InfoHash infoHash
}
