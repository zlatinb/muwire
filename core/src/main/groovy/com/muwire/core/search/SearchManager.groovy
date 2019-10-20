package com.muwire.core.search

import com.muwire.core.EventBus
import com.muwire.core.Persona

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
public class SearchManager {

    private static final int EXPIRE_TIME = 60 * 1000 * 1000
    private static final int CHECK_INTERVAL = 60 * 1000

    private final EventBus eventBus
    private final Persona me
    private final ResultsSender resultsSender
    private final Map<UUID, QueryEvent> responderAddress = Collections.synchronizedMap(new HashMap<>())

    SearchManager(){}

    SearchManager(EventBus eventBus, Persona me, ResultsSender resultsSender) {
        this.eventBus = eventBus
        this.me = me
        this.resultsSender = resultsSender
        Timer timer = new Timer("query-expirer", true)
        timer.schedule({cleanup()} as TimerTask, CHECK_INTERVAL, CHECK_INTERVAL)
    }

    void onQueryEvent(QueryEvent event) {
        if (responderAddress.containsKey(event.searchEvent.uuid)) {
            log.info("Dropping duplicate search uuid $event.searchEvent.uuid")
            return
        }
        responderAddress.put(event.searchEvent.uuid, event)
        eventBus.publish(event.searchEvent)
    }

    void onResultsEvent(ResultsEvent event) {
        Destination target = responderAddress.get(event.uuid)?.replyTo
        if (target == null)
            throw new IllegalStateException("UUID unknown $event.uuid")
        if (event.results.length == 0) {
            log.info("No results for search uuid $event.uuid")
            return
        }
        resultsSender.sendResults(event.uuid, event.results, target, event.searchEvent.oobInfohash, event.searchEvent.compressedResults)
    }

    boolean hasLocalSearch(UUID uuid) {
        me.destination.equals(responderAddress.get(uuid)?.replyTo)
    }

    private void cleanup() {
        final long now = System.currentTimeMillis()
        synchronized(responderAddress) {
            for (Iterator<UUID> iter = responderAddress.keySet().iterator(); iter.hasNext();) {
                UUID uuid = iter.next()
                QueryEvent event = responderAddress.get(uuid)
                if (event.timestamp < now - EXPIRE_TIME)
                    iter.remove()
            }
        }
    }
}
