package com.muwire.core.hostcache

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate
import java.util.function.Predicate
import java.util.function.Supplier

import com.muwire.core.MuWireSettings
import com.muwire.core.Service
import com.muwire.core.connection.ConnectionAttemptStatus
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService

import com.muwire.core.RouterConnectedEvent
import com.muwire.core.RouterDisconnectedEvent

import net.i2p.data.Destination

abstract class HostCache extends Service {
    
    protected final TrustService trustService
    
    protected final MuWireSettings settings
    private final Destination myself
    private volatile boolean routerConnected
    
    protected HostCache(TrustService trustService, MuWireSettings settings, Destination myself) {
        this.trustService = trustService
        this.settings = settings
        this.myself = myself
    }
    
    protected final boolean allowHost(Destination host) {
        if (host == myself)
            return false
        TrustLevel trust = trustService.getLevel(host)
        switch(trust) {
            case TrustLevel.DISTRUSTED :
                return false
            case TrustLevel.TRUSTED :
                return true
            case TrustLevel.NEUTRAL :
                return settings.allowUntrusted()
        }
        false
    }
    
    void onHostDiscoveredEvent(HostDiscoveredEvent e) {
        if (myself == e.destination)
            return
        hostDiscovered(e.destination, e.fromHostcache)
    }
    
    protected abstract void hostDiscovered(Destination d, boolean fromHostcache)
    
    void onConnectionEvent(ConnectionEvent e) {
        if (e.leaf || !routerConnected)
            return
        onConnection(e.endpoint.destination, e.status)
    }

    void onRouterConnectedEvent(RouterConnectedEvent e) {
        routerConnected = true
    }

    void onRouterDisconnectedEvent(RouterDisconnectedEvent e) {
        routerConnected = false
    }
    
    protected abstract void onConnection(Destination d, ConnectionAttemptStatus status)
    
    abstract List<Destination> getHosts(int n, Predicate<Destination> filter)
    abstract List<Destination> getGoodHosts(int n)
    
    abstract int countAllHosts()
    abstract int countFailingHosts()
    abstract int countHopelessHosts()
    
    public abstract void start(Supplier<Collection<Destination>> connected)
    abstract void stop()
}
