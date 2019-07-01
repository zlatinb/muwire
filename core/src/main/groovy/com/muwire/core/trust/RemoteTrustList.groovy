package com.muwire.core.trust

import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.Persona

import net.i2p.util.ConcurrentHashSet

class RemoteTrustList {
    private final Persona persona
    private final Set<Persona> good, bad
    long timestamp
    
    RemoteTrustList(Persona persona) {
        this.persona = persona
        good = new ConcurrentHashSet<>()
        bad = new ConcurrentHashSet<>()
    }
}
