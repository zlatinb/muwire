package com.muwire.core.search

import com.muwire.core.download.SourceVerifiedEvent
import com.muwire.core.util.FixedSizeFIFOSet

import net.i2p.data.Destination

/**
 * Caches destinations that have recently responded to with results.
 */ 
class ResponderCache {

    private final FixedSizeFIFOSet<Destination> cache
    
    ResponderCache(int capacity) {
        cache = new FixedSizeFIFOSet<>(capacity)
    }
    
    synchronized void onUIResultBatchEvent(UIResultBatchEvent e) {
        cache.add(e.results[0].sender.destination)
    }
    
    synchronized void onSourceVerifiedEvent(SourceVerifiedEvent e) {
        cache.add(e.source)
    }
    
    synchronized boolean hasResponded(Destination d) {
        cache.contains(d)
    }
}
