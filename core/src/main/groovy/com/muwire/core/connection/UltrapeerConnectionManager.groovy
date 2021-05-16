package com.muwire.core.connection

import java.util.Collection
import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.hostcache.HostCache
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.ResponderCache
import com.muwire.core.trust.TrustService

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
class UltrapeerConnectionManager extends ConnectionManager {
    
    private static final int QUERY_FACTOR = 2

    final int maxPeers, maxLeafs
    final TrustService trustService
    final ResponderCache responderCache

    final Map<Destination, PeerConnection> peerConnections = new ConcurrentHashMap()
    final Map<Destination, LeafConnection> leafConnections = new ConcurrentHashMap()
    
    private final Random random = new Random()

    UltrapeerConnectionManager() {}

    public UltrapeerConnectionManager(EventBus eventBus, Persona me, int maxPeers, int maxLeafs,
        HostCache hostCache, ResponderCache responderCache, TrustService trustService, MuWireSettings settings) {
        super(eventBus, me, hostCache, settings)
        this.maxPeers = maxPeers
        this.maxLeafs = maxLeafs
        this.trustService = trustService
        this.responderCache = responderCache
    }
    @Override
    public void drop(Destination d) {
        peerConnections.get(d)?.close()
        leafConnections.get(d)?.close()
    }

    void onQueryEvent(QueryEvent e) {
        forwardQueryToLeafs(e)
        if (!e.firstHop)
            return
        if (e.replyTo != me.destination && e.receivedOn != me.destination &&
            !leafConnections.containsKey(e.receivedOn))
            e.firstHop = false
        final int connCount = peerConnections.size()
        if (connCount == 0)
            return
        final int treshold = QUERY_FACTOR * ((int)(Math.sqrt(connCount)) + 1)
        peerConnections.values().each {
            // 1. do not send query back to originator
            // 2. if firstHop forward to everyone
            // 3. otherwise to everyone who has recently responded/transferred to us + randomized sqrt of neighbors
            if (e.getReceivedOn() != it.getEndpoint().getDestination() &&
                (e.firstHop || 
                    responderCache.hasResponded(it.endpoint.destination) ||
                    random.nextInt(connCount) < treshold))
                it.sendQuery(e)
        }
    }

    @Override
    public Collection<Connection> getConnections() {
        def rv = new ArrayList(peerConnections.size() + leafConnections.size())
        rv.addAll(peerConnections.values())
        rv.addAll(leafConnections.values())
        rv
    }

    boolean hasLeafSlots() {
        leafConnections.size() < maxLeafs
    }

    boolean hasPeerSlots() {
        peerConnections.size() < maxPeers
    }

    @Override
    protected int getDesiredConnections() {
        return maxPeers / 2;
    }
    @Override
    public boolean isConnected(Destination d) {
        peerConnections.containsKey(d) || leafConnections.containsKey(d)
    }

    @Override
    public void onConnectionEvent(ConnectionEvent e) {
        if (!e.incoming && e.leaf) {
            log.severe("Inconsistent event $e")
            return
        }

        if (e.status != ConnectionAttemptStatus.SUCCESSFUL)
            return

        Connection c = e.leaf ?
            new LeafConnection(eventBus, e.endpoint, hostCache, trustService, settings) :
            new PeerConnection(eventBus, e.endpoint, e.incoming, hostCache, trustService, settings)
        def map = e.leaf ? leafConnections : peerConnections
        map.put(e.endpoint.destination, c)
        c.start()
    }

    @Override
    public void onDisconnectionEvent(DisconnectionEvent e) {
        def removed = peerConnections.remove(e.destination)
        if (removed == null)
            removed = leafConnections.remove(e.destination)
        if (removed == null)
            log.severe("Removed connection not present in either leaf or peer map ${e.destination.toBase32()}")
    }

    @Override
    void shutdown() {
        super.shutdown()
        peerConnections.values().stream().forEach({v -> v.close()})
        leafConnections.values().stream().forEach({v -> v.close()})
        peerConnections.clear()
        leafConnections.clear()
    }

    void forwardQueryToLeafs(QueryEvent e) {

    }
}
