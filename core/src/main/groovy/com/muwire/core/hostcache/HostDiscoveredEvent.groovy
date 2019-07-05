package com.muwire.core.hostcache

import com.muwire.core.Event

import net.i2p.data.Destination

class HostDiscoveredEvent extends Event {

    Destination destination
    boolean fromHostcache

    @Override
    public String toString() {
        "HostDiscoveredEvent ${super.toString()} destination:${destination.toBase32()} from hostcache $fromHostcache"
    }
}
