package com.muwire.core.search

import com.muwire.core.SharedFile

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
class ResultsSender {
    
    void sendResults(UUID uuid, SharedFile[] results, Destination target) {
        log.info("Sending $results.length results for uuid $uuid to ${target.toBase32()}")    
    }
}
