package com.muwire.core.search

import com.muwire.core.SharedFile
import com.muwire.core.Persona
import com.muwire.core.EventBus

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
class ResultsSender {
    
    private final Persona me
    private final EventBus eventBus
    
    ResultsSender(EventBus eventBus, Persona me) {
        this.eventBus = eventBus
        this.me = me
    }
    
    void sendResults(UUID uuid, SharedFile[] results, Destination target) {
        log.info("Sending $results.length results for uuid $uuid to ${target.toBase32()}")
        if (target.equals(me.destination)) {
            def resultEvent = new ResultsEvent( uuid : uuid, results : results )
            def uiResultEvent = new UIResultEvent(resultsEvent : resultEvent)
            eventBus.publish(uiResultEvent)
        }
    }
}
