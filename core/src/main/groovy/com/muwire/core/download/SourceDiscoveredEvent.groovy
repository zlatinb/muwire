package com.muwire.core.download

import com.muwire.core.Event
import com.muwire.core.InfoHash
import com.muwire.core.Persona

class SourceDiscoveredEvent extends Event {
    InfoHash infoHash
    Persona source
}
