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
    
    private final Map<InfoHash, Swarm> swarms = new ConcurrentHashMap<>()
    
    @PostConstruct
    public void postConstruct() {
        core.eventBus.register(UIResultBatchEvent.class, this)
    }
    
    void onUIResultBatchEvent(UIResultBatchEvent e) {
        InfoHash ih = e.results[0].infohash
        Swarm swarm = swarms.get(ih)
        if (swarm == null) {
            log.warning("no swarm found for result with infoHash $ih")
            return
        }
        
        swarm.add(e.results[0].sender)            
    }
    
    void track(InfoHash infoHash) {
        Swarm swarm = swarms.computeIfAbsent(infoHash, {new Swarm(it)} as Function)
        if (swarm.needsQuery()) {
            UUID uuid = UUID.randomUUID()
            def searchEvent = new SearchEvent(searchHash : infoHash.getRoot(), uuid: uuid, oobInfohash: true, compressedResults : true, persona : core.me)
            byte [] payload = infoHash.getRoot()
            boolean firstHop = core.muOptions.allowUntrusted || core.muOptions.searchExtraHop
            
            Signature sig = DSAEngine.getInstance().sign(payload, core.spk) 
            long timestamp = System.currentTimeMillis()
            core.eventBus.publish(new QueryEvent(searchEvent : searchEvent, firstHop : firstHop,
            replyTo: core.me.destination, receivedOn: core.me.destination,
            originator : core.me, sig : sig.data, queryTime : timestamp, sig2 : DataUtil.signUUID(uuid, timestamp, core.spk)))
        }
    }
}
