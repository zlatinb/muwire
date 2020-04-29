package com.muwire.tracker

import java.util.function.Function

import com.muwire.core.InfoHash
import com.muwire.core.Persona

import groovy.util.logging.Log

/**
 * A swarm for a given file
 */
@Log
class Swarm {
    final InfoHash infoHash
    
    /** 
     * Invariant: these four collections are mutually exclusive.
     * A given host can be only in one of them at the same time.
     */
    private final Map<Persona,Host> seeds = new HashMap<>()
    private final Map<Persona,Host> leeches = new HashMap<>()
    private final Map<Persona,Host> unknown = new HashMap<>()
    private final Set<Persona> negative = new HashSet<>()
    
    /**
     * hosts which are currently being pinged.  Hosts can be in here
     * and in the collections above, except for negative.
     */
    private final Map<Persona, Host> inFlight = new HashMap<>()
    
    /**
     * Last time a query was made to the MW network for this hash
     */
    private long lastQueryTime
    
    /**
     * Last time a batch of hosts was pinged
     */
    private long lastPingTime
    
    Swarm(InfoHash infoHash) {
        this.infoHash = infoHash
    }
    
    /**
     * @param cutoff expire hosts older than this
     */
    synchronized void expire(long cutoff, int maxFailures) {
        doExpire(cutoff, maxFailures, seeds)
        doExpire(cutoff, maxFailures, leeches)
        doExpire(cutoff, maxFailures, unknown)
    }
    
    private static void doExpire(long cutoff, int maxFailures, Map<Persona,Host> map) {
        for (Iterator<Persona> iter = map.keySet().iterator(); iter.hasNext();) {
            Persona p = iter.next()
            Host h = map.get(p)
            if (h.isExpired(cutoff, maxFailures))
                iter.remove()
        }
    }
    
    synchronized boolean shouldQuery(long queryCutoff, long now) {
        if (!(seeds.isEmpty() &&
            leeches.isEmpty() &&
            inFlight.isEmpty() &&
            unknown.isEmpty()))
        return false 
        if (lastQueryTime <= queryCutoff) {
            lastQueryTime = now
            return true
        }
        false
    }
    
    synchronized boolean isHealthy() {
        !seeds.isEmpty()
        // TODO add xHave accumulation of leeches 
    }
    
    synchronized void add(Persona p) {
        if (!(seeds.containsKey(p) || leeches.containsKey(p) || 
            negative.contains(p) || inFlight.containsKey(p)))
            unknown.computeIfAbsent(p, {new Host(it)} as Function)
    }
    
    synchronized void handleResponse(Host responder, int code) {
        Host h = inFlight.remove(responder.persona)
        if (responder != h)
            log.warning("received a response mismatch from host $responder vs $h")
            
        responder.lastResponded = System.currentTimeMillis()
        responder.failures = 0    
        switch(code) {
            case 200: addSeed(responder); break
            case 206 : addLeech(responder); break;
            default :
            addNegative(responder)
        }
    }
    
    synchronized void fail(Host failed) {
        Host h = inFlight.remove(failed.persona)
        if (h != failed)
            log.warning("failed a host that wasn't in flight $failed vs $h")
        h.failures++
    }
    
    private void addSeed(Host h) {
        leeches.remove(h.persona)
        unknown.remove(h.persona)
        seeds.put(h.persona, h)
    }
    
    private void addLeech(Host h) {
        unknown.remove(h.persona)
        seeds.remove(h.persona)
        leeches.put(h.persona, h)
    }
    
    private void addNegative(Host h) {
        unknown.remove(h.persona)
        seeds.remove(h.persona)
        leeches.remove(h.persona)
        negative.add(h.persona)
    }
    
    /**
     * @param max number of hosts to give back
     * @param now what time is it now
     * @param cutoff only consider hosts which have been pinged before this time
     * @return hosts to be pinged
     */
    synchronized List<Host> getBatchToPing(int max, long now, long cutOff) {        
        List<Host> rv = new ArrayList<>()
        rv.addAll(unknown.values())
        rv.addAll(seeds.values())
        rv.addAll(leeches.values())
        rv.removeAll(inFlight.values())
        
        rv.removeAll { it.lastPinged >= cutOff }
        
        Collections.sort(rv, {l, r ->
            Long.compare(l.lastPinged, r.lastPinged)
        } as Comparator<Host>)
        
        if (rv.size() > max)
            rv = rv[0..(max-1)]
        
        rv.each {
            it.lastPinged = now
            inFlight.put(it.persona, it)
        }
        
        if (!rv.isEmpty())
            lastPingTime = now
        rv
    }
    
    synchronized long getLastPingTime() {
        lastPingTime
    }
    
    public Info info() {
        List<String> seeders = seeds.keySet().collect { it.getHumanReadableName() }
        List<String> leechers = leeches.keySet().collect { it.getHumanReadableName() }
        return new Info(seeders, leechers, unknown.size(), negative.size())
    }
    
    public static class Info {
        final List<String> seeders, leechers
        final int unknown, negative
        
        Info(List<String> seeders, List<String> leechers, int unknown, int negative) {
            this.seeders = seeders
            this.leechers = leechers
            this.unknown = unknown
            this.negative = negative
        }
    }
}
