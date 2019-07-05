package com.muwire.core.download

import com.muwire.core.Event
import com.muwire.core.search.UIResultEvent

import net.i2p.data.Destination

class UIDownloadEvent extends Event {

    UIResultEvent[] result
    Set<Destination> sources
    File target
}
