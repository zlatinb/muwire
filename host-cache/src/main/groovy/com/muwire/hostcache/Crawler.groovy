package com.muwire.hostcache

import java.util.stream.Collectors

import net.i2p.data.Destination

class Crawler {

    final def pinger
    final def hostPool
	final int parallel
    
    final Map<Destination, Host> inFlight = new HashMap<>()
    
    UUID currentUUID
    
    Crawler(pinger, hostPool, int parallel) {
        this.pinger = pinger
        this.hostPool = hostPool
		this.parallel = parallel
    }
    
    synchronized def handleCrawlerPong(pong, Destination source) {
        if (!inFlight.containsKey(source)) {
            return
        }
        Host host = inFlight.remove(source)
        
        if (pong.uuid == null || pong.leafSlots == null || pong.peerSlots == null || pong.peers == null) {
            hostPool.fail(host)
            return
        }
        
        UUID uuid;
        try {
            uuid = UUID.fromString(pong.uuid)
        } catch (IllegalArgumentException bad) {
            hostPool.fail(host)
            return
        }
        
        if (!uuid.equals(currentUUID)) {
            hostPool.fail(host)
            return
        }
        
        host.leafSlots = parseBoolean(pong.leafSlots)
        host.peerSlots = parseBoolean(pong.peerSlots)
        
        def peers
        try {
            peers = pong.peers.stream().map({b64 -> new Destination(b64)}).collect(Collectors.toSet())
        } catch (Exception e) {
            hostPool.fail(host)
            return
        }
        peers.each {
            def newHost = new Host()
            newHost.destination = it
            hostPool.addUnverified(newHost)
        }
        hostPool.verify(host)
    }
    
    private static boolean parseBoolean(value) {
        return Boolean.parseBoolean(value.toString())
    }
	
	synchronized def startCrawl() {
		if (!inFlight.isEmpty()) {
			inFlight.values().each { hostPool.fail(it) }
			inFlight.clear()
		}
		currentUUID = UUID.randomUUID()
		hostPool.getUnverified(parallel).each { 
			inFlight.put(it.destination, it)
			pinger.ping(it, currentUUID)
		}
	}
}
