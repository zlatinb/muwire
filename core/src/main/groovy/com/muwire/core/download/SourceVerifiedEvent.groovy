package com.muwire.core.download

import com.muwire.core.Event
import com.muwire.core.InfoHash

import net.i2p.data.Destination

class SourceVerifiedEvent extends Event {
    InfoHash infoHash
    Destination source
}
