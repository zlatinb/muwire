package com.muwire.core.search

import com.muwire.core.EventBus
import com.muwire.core.Persona

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
public class SearchManager {
    
    private final EventBus eventBus
    private final Persona me
    private final ResultsSender resultsSender
    private final Map<UUID, Destination> responderAddress = new HashMap<>()
    
    SearchManager(){}
    
    SearchManager(EventBus eventBus, Persona me, ResultsSender resultsSender) {
        this.eventBus = eventBus
        this.me = me
        this.resultsSender = resultsSender
    }
    
    void onQueryEvent(QueryEvent event) {
        // TODO: duplicate UUID check
        responderAddress.put(event.searchEvent.uuid, event.replyTo)
        eventBus.publish(event.searchEvent)
    }
    
    void onResultsEvent(ResultsEvent event) {
        Destination target = responderAddress.get(event.uuid)
        if (target == null)
            throw new IllegalStateException("UUID unknown $event.uuid")
        if (event.results.length == 0) {
            log.info("No results for search uuid $event.uuid")
            return
        }
        resultsSender.sendResults(event.uuid, event.results, target)
    }
    
    boolean hasLocalSearch(UUID uuid) {
        me.destination.equals(responderAddress.get(uuid))
    }
}
