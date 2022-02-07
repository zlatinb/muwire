package com.muwire.core.search

import com.muwire.core.util.MessageThrottle

import com.muwire.core.EventBus
import com.muwire.core.Persona
import com.muwire.core.SharedFile
import com.muwire.core.util.SenderThrottle
import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
public class SearchManager {

    private static final int EXPIRE_TIME = 60 * 1000 * 1000
    private static final int CHECK_INTERVAL = 60 * 1000
    private static final int RESULT_DELAY = 100
    
    private static final long REGEX_INTERVAL = 1000
    private static final int TOTAL_ALLOWED_REGEX = 5
    private static final int SENDER_ALLOWED_REGEX = 2

    private final EventBus eventBus
    private final Persona me
    private final ResultsSender resultsSender
    private final Map<UUID, QueryEvent> responderAddress = Collections.synchronizedMap(new HashMap<>())
    
    private final Map<UUID, ResultBatch> pendingResults = new HashMap<>()
    
    private final Object throttleLock = new Object()
    private final MessageThrottle globalRegexThrottle = new MessageThrottle(REGEX_INTERVAL, TOTAL_ALLOWED_REGEX)
    private final SenderThrottle senderRegexThrottle = new SenderThrottle(REGEX_INTERVAL, SENDER_ALLOWED_REGEX)

    SearchManager(){}

    SearchManager(EventBus eventBus, Persona me, ResultsSender resultsSender) {
        this.eventBus = eventBus
        this.me = me
        this.resultsSender = resultsSender
        Timer timer = new Timer("query-expirer", true)
        timer.schedule({cleanup()} as TimerTask, CHECK_INTERVAL, CHECK_INTERVAL)
        timer.schedule({sendBatched()}, RESULT_DELAY, RESULT_DELAY)
    }

    void onQueryEvent(QueryEvent event) {
        if (responderAddress.containsKey(event.searchEvent.uuid)) {
            log.info("Dropping duplicate search uuid $event.searchEvent.uuid")
            return
        }
        
        if (event.searchEvent.regex) {
            final long now = System.currentTimeMillis()
            synchronized (throttleLock) {
                if (!globalRegexThrottle.allow(now)) {
                    log.info("regex query over global limit $event")
                    return
                }
                if (!senderRegexThrottle.allow(now, event.getOriginator())) {
                    log.info("regex query over sender limi $event")
                    return
                }
            }
        }
        
        responderAddress.put(event.searchEvent.uuid, event)
        eventBus.publish(event.searchEvent)
    }

    synchronized void onResultsEvent(ResultsEvent event) {
        Destination target = responderAddress.get(event.uuid)?.replyTo
        if (target == null)
            throw new IllegalStateException("UUID unknown $event.uuid")
        if (event.results.length == 0) {
            log.info("No results for search uuid $event.uuid")
            return
        }
        
        ResultBatch batch = pendingResults.putIfAbsent(event.uuid, new ResultBatch(event, target))
        if (batch != null)
            batch.results.addAll(event.results)
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
        synchronized (throttleLock) {
            senderRegexThrottle.clear(now)
        }
    }
    
    private synchronized void sendBatched() {
        if (pendingResults.isEmpty())
            return
            
        Set<ResultBatch> copy = new HashSet<>(pendingResults.values())
        pendingResults.clear()
        
        copy.each { 
            SharedFile[] results = it.results.toArray()
            resultsSender.sendResults(it.resultsEvent.uuid, results, it.target, 
                it.resultsEvent.searchEvent.oobInfohash, 
                it.resultsEvent.searchEvent.compressedResults,
                it.resultsEvent.searchEvent.searchPaths
            )
        }
    }
    
    private static class ResultBatch {
        final ResultsEvent resultsEvent
        final Destination target
        final Set<SharedFile> results = new HashSet<>()
        ResultBatch(ResultsEvent resultsEvent, Destination target) {
            this.resultsEvent = resultsEvent
            this.target = target
            results.addAll(resultsEvent.results)
        }
    }
}
