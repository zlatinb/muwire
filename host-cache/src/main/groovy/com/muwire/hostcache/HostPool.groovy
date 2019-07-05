package com.muwire.hostcache

import java.util.stream.Collectors

import groovy.json.JsonOutput

class HostPool {
    
    final def maxFailures
    final def maxAge
    
    def verified = new HashMap()
    def unverified = new HashMap()
    
    HostPool() {}
    HostPool(maxFailures, maxAge) {
        this.maxAge = maxAge
        this.maxFailures = maxFailures
    }
    
    synchronized def getVerified(int max, boolean leaf) {
        if (verified.isEmpty()) {
            return Collections.emptyList()
        }
        def asList = verified.values().stream().filter({ it -> leaf ? it.leafSlots : it.peerSlots}).collect(Collectors.toList())
        Collections.shuffle(asList)

        return asList[0..Math.min(max, asList.size()) -1]
    }
    
    synchronized def addUnverified(host) {
        if (!verified.containsKey(host.destination)) {
            unverified.put(host.destination, host)
        }
    }
    
    synchronized def getUnverified(int max) {
        if (unverified.isEmpty()) {
            return Collections.emptyList()
        }
        def asList = unverified.values().asList()
        Collections.shuffle(asList)
        return asList[0..(Math.min(max, asList.size())-1)]
    }
    
    synchronized def verify(host) {
        if (!unverified.remove(host.destination))
            throw new IllegalArgumentException()
        host.verifyTime = System.currentTimeMillis();
        host.verificationFailures = 0
        verified.put(host.destination, host)
    }
    
    synchronized def fail(host) {
        if (!unverified.containsKey(host.destination))
            return
        host.verificationFailures++
    }
    
    synchronized def age() {
        final long now = System.currentTimeMillis()
        for (Iterator iter = verified.keySet().iterator(); iter.hasNext();) {
            def destination = iter.next()
            def host = verified.get(destination)
            if (host.verifyTime + maxAge < now) {
                iter.remove()
                unverified.put(host.destination, host)
            }
        }
        
        for (Iterator iter = unverified.keySet().iterator(); iter.hasNext();) {
            def destination = iter.next()
            def host = unverified.get(destination)
            if (host.verificationFailures >= maxFailures) {
                iter.remove()
            }
        }
    }
    
    synchronized void serialize(File verifiedFile, File unverifiedFile) {
        write(verifiedFile, verified.values())
        write(unverifiedFile, unverified.values())
    }
    
    private void write(File target, Collection hosts) {
        JsonOutput jsonOutput = new JsonOutput()
        target.withPrintWriter { writer ->
            hosts.each { 
                def json = [:]
                json.destination = it.destination.toBase64()
                json.verifyTime = it.verifyTime
                json.leafSlots = it.leafSlots
                json.peerSlots = it.peerSlots
                json.verificationFailures = it.verificationFailures
                def str = jsonOutput.toJson(json)
                writer.println(str)
            } 
        }
    }
}
