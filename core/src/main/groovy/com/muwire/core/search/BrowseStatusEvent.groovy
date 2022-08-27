package com.muwire.core.search

import com.muwire.core.Event
import com.muwire.core.Persona

class BrowseStatusEvent extends Event {
    Persona host
    BrowseStatus status
    BrowseSession session
    int totalResults
    UUID uuid
}
