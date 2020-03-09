package com.muwire.core.filefeeds

import com.muwire.core.Event
import com.muwire.core.Persona

class FeedFetchEvent extends Event {
    Persona host
    FeedFetchStatus status
    int totalItems
}
