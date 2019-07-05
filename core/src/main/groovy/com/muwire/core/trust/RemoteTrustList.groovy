package com.muwire.core.trust

import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.Persona

import net.i2p.util.ConcurrentHashSet

class RemoteTrustList {
    public enum Status { NEW, UPDATING, UPDATED, UPDATE_FAILED }

    private final Persona persona
    private final Set<Persona> good, bad
    volatile long timestamp
    volatile boolean forceUpdate
    Status status = Status.NEW

    RemoteTrustList(Persona persona) {
        this.persona = persona
        good = new ConcurrentHashSet<>()
        bad = new ConcurrentHashSet<>()
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RemoteTrustList))
            return false
        RemoteTrustList other = (RemoteTrustList)o
        persona == other.persona
    }
}
