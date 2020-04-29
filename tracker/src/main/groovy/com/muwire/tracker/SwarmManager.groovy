package com.muwire.tracker

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.search.UIResultBatchEvent
import com.muwire.core.util.DataUtil

import groovy.util.logging.Log
import net.i2p.crypto.DSAEngine
import net.i2p.data.Signature

@Component
@Log
class SwarmManager {
    @Autowired
    private Core core
    
    @Autowired
    private Pinger pinger
    
    @Autowired
    private TrackerProperties trackerProperties
    
    private final Map<InfoHash, Swarm> swarms = new ConcurrentHashMap<>()
    private final Map<UUID, InfoHash> queries = new ConcurrentHashMap<>()
    private final Timer swarmTimer = new Timer("swarm-timer",true)
    
    @PostConstruct
    public void postConstruct() {
        core.eventBus.register(UIResultBatchEvent.class, this)
        swarmTimer.schedule({trackSwarms()} as TimerTask, 10 * 1000, 10 * 1000)
    }
    
    void onUIResultBatchEvent(UIResultBatchEvent e) {
        InfoHash stored = queries.get(e.uuid)
        InfoHash ih = e.results[0].infohash
        
        if (ih != stored) {
            log.warning("infohash mismatch in result $ih vs $stored")
            return
        }
        
        Swarm swarm = swarms.get(ih)
        if (swarm == null) {
            log.warning("no swarm found for result with infoHash $ih")
            return
        }
        
        log.info("got a result with uuid ${e.uuid} for infoHash $ih")
        swarm.add(e.results[0].sender)            
    }
    
    int countSwarms() {
        swarms.size()
    }
    
    private void trackSwarms() {
        final long now = System.currentTimeMillis()
        final long expiryCutoff = now - trackerProperties.getSwarmParameters().getExpiry() * 60 * 1000L
        swarms.values().each { it.expire(expiryCutoff) }
        final long queryCutoff = now - trackerProperties.getSwarmParameters().getQueryInterval() * 60 * 60 * 1000L
        swarms.values().each { 
            if (it.shouldQuery(queryCutoff, now))
                query(it)
        }
    }
    
    private void query(Swarm swarm) {
        InfoHash infoHash = swarm.getInfoHash()
        cleanQueryMap(infoHash)
        UUID uuid = UUID.randomUUID()
        queries.put(uuid, infoHash)
        
        log.info("will query MW network for $infoHash with uuid $uuid")
        
        def searchEvent = new SearchEvent(searchHash : infoHash.getRoot(), uuid: uuid, oobInfohash: true, compressedResults : true, persona : core.me)
        byte [] payload = infoHash.getRoot()
        boolean firstHop = core.muOptions.allowUntrusted || core.muOptions.searchExtraHop

        Signature sig = DSAEngine.getInstance().sign(payload, core.spk)
        long timestamp = System.currentTimeMillis()
        core.eventBus.publish(new QueryEvent(searchEvent : searchEvent, firstHop : firstHop,
        replyTo: core.me.destination, receivedOn: core.me.destination,
        originator : core.me, sig : sig.data, queryTime : timestamp, sig2 : DataUtil.signUUID(uuid, timestamp, core.spk)))
    }
    
    void track(InfoHash infoHash) {
        swarms.computeIfAbsent(infoHash, {new Swarm(it)} as Function)
    }
    
    boolean forget(InfoHash infoHash) {
        Swarm swarm = swarms.remove(infoHash)
        if (swarm != null) {
            cleanQueryMap(infoHash)
            return true
        } else
            return false
    }
    
    private void cleanQueryMap(InfoHash infoHash) {
        queries.values().removeAll {it == infoHash}
    }
    
    Swarm.Info info(InfoHash infoHash) {
        swarms.get(infoHash)?.info()
    }
}
