package com.muwire.core.search

import com.muwire.core.Event
import com.muwire.core.InfoHash
import com.muwire.core.Persona

class UIResultEvent extends Event {
    Persona sender
    UUID uuid
    String name
    long size
    InfoHash infohash
    int pieceSize
}
