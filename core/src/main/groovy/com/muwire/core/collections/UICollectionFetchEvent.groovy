package com.muwire.core.collections

import com.muwire.core.Event
import com.muwire.core.InfoHash
import com.muwire.core.Persona

class UICollectionFetchEvent extends Event {
    UUID uuid
    Persona host
    Set<InfoHash> infoHashes
}
