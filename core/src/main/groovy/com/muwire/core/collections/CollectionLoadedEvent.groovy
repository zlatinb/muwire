package com.muwire.core.collections

import com.muwire.core.Event

class CollectionLoadedEvent extends Event {
    FileCollection collection
    boolean local
}
