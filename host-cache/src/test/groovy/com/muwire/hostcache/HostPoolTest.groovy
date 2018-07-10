package com.muwire.hostcache

import org.junit.*

class HostPoolTest {
    
    def hp
    Host freeLeafs, freePeers, freeBoth, freeNone
    
    @Before
    void before() {
        hp = new HostPool(3, 10)
        
        freeLeafs = new Host()
        freeLeafs.destination = "freeLeafs"
        freeLeafs.leafSlots = true
        
        freePeers = new Host()
        freePeers.destination = "freePeers"
        freePeers.peerSlots = true
        
        freeBoth = new Host()
        freeBoth.destination = "freeBoth"
        freeBoth.leafSlots = true
        freeBoth.peerSlots = true
        
        freeNone = new Host()
        freeNone.destination = "freeNone"
    }
    
    def addAllUnverified() {
        hp.addUnverified(freeLeafs)
        hp.addUnverified(freePeers)
        hp.addUnverified(freeBoth)
        hp.addUnverified(freeNone)
    }
    
    def verifyAll() {
        hp.verify(freeBoth)
        hp.verify(freeLeafs)
        hp.verify(freePeers)
        hp.verify(freeNone)
    }
    
    @Test
    void testNoVerified() { 
        assert hp.getVerified(1, true).isEmpty()
        assert hp.getVerified(1, false).isEmpty()

        addAllUnverified()        
        
        assert hp.getVerified(1, true).isEmpty()
        assert hp.getVerified(1, false).isEmpty()
    }
    
    @Test
    void testOneVerified() {
        addAllUnverified()
        hp.verify(freeBoth)
        
        assert hp.getVerified(10, true).contains(freeBoth)
        assert hp.getVerified(10, false).contains(freeBoth)
    }
    
    @Test
    void testFilterByType() {
        addAllUnverified()
        verifyAll()
        def verifiedLeafSlots = hp.getVerified(10, true)
        assert verifiedLeafSlots.size() == 2
        assert verifiedLeafSlots.contains(freeBoth)
        assert verifiedLeafSlots.contains(freeLeafs)
        
        def verifiedPeerSlots = hp.getVerified(10, false)
        assert verifiedPeerSlots.size() == 2
        assert verifiedPeerSlots.contains(freeBoth)
        assert verifiedPeerSlots.contains(freePeers)
    }
    
    @Test
    void getFewerThanAvailable() {
        addAllUnverified()
        verifyAll()
        def verifiedLeafSlots = hp.getVerified(1, true)
        assert verifiedLeafSlots.size() == 1
        assert verifiedLeafSlots.contains(freeBoth) || verifiedLeafSlots.contains(freeLeafs)
    }
    
    @Test
    void getUnverified() {
        assert hp.getUnverified().isEmpty()
        
        addAllUnverified()
        
        def allUnverified = hp.getUnverified(10)
        assert allUnverified.size() == 4
        assert allUnverified.contains(freeLeafs)
        assert allUnverified.contains(freePeers)
        assert allUnverified.contains(freeBoth)
        assert allUnverified.contains(freeNone)
        
        def twoUnverified = hp.getUnverified(2)
        assert twoUnverified.size() == 2
        
        def oneUnverified = hp.getUnverified(1)
        assert oneUnverified.size() == 1
        assert oneUnverified.contains(freeLeafs) || oneUnverified.contains(freePeers) ||
            oneUnverified.contains(freeBoth) || oneUnverified.contains(freeNone)
    }
    
    @Test
    void testFailHost() {
        hp.addUnverified(freeBoth)
        assert hp.getUnverified(10).size() == 1
 
        hp.fail(freeBoth)
        hp.age()
        assert hp.getUnverified(10).size() == 1
        
        hp.fail(freeBoth)
        hp.age()
        assert hp.getUnverified(10).size() == 1
        
        hp.fail(freeBoth)
        hp.age()
        assert hp.getUnverified(10).isEmpty()
        assert hp.getVerified(10, true).isEmpty()
    }
    
    @Test
    void verifyResetsFailures() {
        hp.addUnverified(freeBoth)
        assert hp.getUnverified(10).size() == 1
 
        hp.fail(freeBoth)
        hp.age()
        assert hp.getUnverified(10).size() == 1
       
        hp.fail(freeBoth)
        hp.age()
        assert hp.getUnverified(10).size() == 1

        hp.verify(freeBoth)        
        hp.age()
        assert hp.getUnverified(10).isEmpty()
        assert hp.getVerified(10, true).size() == 1
    }
    
    @Test
    void ageHost() {
        hp.addUnverified(freeBoth)
        hp.verify(freeBoth)
        
        hp.age()
        assert hp.getVerified(10,true).size() == 1
        assert hp.getUnverified(10).isEmpty()
        
        Thread.sleep(20)
        hp.age()
        assert hp.getVerified(10,true).isEmpty()
        assert hp.getUnverified(10).size() == 1
    }
}
