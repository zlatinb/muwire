package com.muwire.core.connection

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.hostcache.HostCache
import com.muwire.core.search.QueryEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

import net.i2p.data.Destination

abstract class ConnectionManager {

    private static final int PING_TIME = 20000

    final EventBus eventBus

    private final Timer timer

    protected final HostCache hostCache
    protected final Persona me
    protected final MuWireSettings settings

    ConnectionManager() {}

    ConnectionManager(EventBus eventBus, Persona me, HostCache hostCache, MuWireSettings settings) {
        this.eventBus = eventBus
        this.me = me
        this.hostCache = hostCache
        this.settings = settings
        this.timer = new Timer("connections-pinger",true)
    }

    void start() {
        timer.schedule({sendPings()} as TimerTask, 1000,1000)
    }

    void shutdown() {
        timer.cancel()
    }

    void onTrustEvent(TrustEvent e) {
        if (e.level == TrustLevel.DISTRUSTED)
            drop(e.persona.destination)
    }

    abstract void drop(Destination d)

    abstract Collection<Connection> getConnections()

    protected abstract int getDesiredConnections()

    boolean needsConnections() {
        return getConnections().size() < getDesiredConnections()
    }

    abstract boolean isConnected(Destination d)

    abstract void onConnectionEvent(ConnectionEvent e)

    abstract void onDisconnectionEvent(DisconnectionEvent e)

    protected void sendPings() {
        final long now = System.currentTimeMillis()
        getConnections().each {
            if (now - it.lastPingSentTime > PING_TIME)
                it.sendPing()
        }
    }
}
