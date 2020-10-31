package com.muwire.core.collections

import com.muwire.core.Event

class CollectionFetchStatusEvent extends Event {
    CollectionFetchStatus status
    int count
    UUID uuid
}
