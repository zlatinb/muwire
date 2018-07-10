package com.muwire.hostcache

class HostPool {
    
    def verified = new HashSet()

    synchronized def getVerified(int max) {
        def asList = verified.asList()
        Collections.shuffle(asList)
        return asList[0..max].collect { it -> it.destination }
    }
    
    synchronized def addUnverified(host) {
        
    }
    
    synchronized def verify(host) {
        
    }
}
