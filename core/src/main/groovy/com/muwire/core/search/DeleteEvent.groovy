package com.muwire.core.search

import com.muwire.core.Event

import net.i2p.data.Base32
import net.i2p.data.Destination

class DeleteEvent extends Event {
    byte [] infoHash
    Destination leaf
    
    @Override
    public String toString() {
        "DeleteEvent ${super.toString()} infoHash:${Base32.encode(infoHash)} leaf:${leaf.toBase32()}"
    }
}
