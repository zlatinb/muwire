package com.muwire.hostcache

import java.util.stream.Collectors

class HostPool {
    
    final def maxFailures
    final def maxAge
    
    def verified = new HashSet()
    def unverified = new HashSet()
    
    HostPool(maxFailures, maxAge) {
        this.maxAge = maxAge
        this.maxFailures = maxFailures
    }
    
    synchronized def getVerified(int max, boolean leaf) {
        if (verified.isEmpty()) {
            return Collections.emptyList()
        }
        def asList = verified.stream().filter({ it -> leaf ? it.leafSlots : it.peerSlots}).collect(Collectors.toList())
        Collections.shuffle(asList)

        return asList[0..Math.min(max, asList.size()) -1]
    }
    
    synchronized def addUnverified(host) {
        if (!verified.contains(host)) {
            unverified.add(host)
        }
    }
    
    synchronized def getUnverified(int max) {
        if (unverified.isEmpty()) {
            return Collections.emptyList()
        }
        def asList = unverified.asList()
        Collections.shuffle(asList)
        return asList[0..(Math.min(max, asList.size())-1)]
    }
    
    synchronized def verify(host) {
        if (!unverified.remove(host))
            throw new IllegalArgumentException()
        host.verifyTime = System.currentTimeMillis();
        host.verificationFailures = 0
        verified.add(host)
    }
    
    synchronized def fail(host) {
        if (!unverified.contains(host))
            throw new IllegalArgumentException()
        host.verificationFailures++
    }
    
    synchronized def age() {
        final long now = System.currentTimeMillis()
        for (Iterator iter = verified.iterator(); iter.hasNext();) {
            def host = iter.next()
            if (host.verifyTime + maxAge < now) {
                iter.remove()
                unverified.add(host)
            }
        }
        
        for (Iterator iter = unverified.iterator(); iter.hasNext();) {
            def host = iter.next()
            if (host.verificationFailures >= maxFailures) {
                iter.remove()
            }
        }
    }
}
