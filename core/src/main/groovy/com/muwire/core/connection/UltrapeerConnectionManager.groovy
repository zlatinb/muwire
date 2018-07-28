package com.muwire.core.connection

import java.util.Collection
import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.EventBus
import com.muwire.core.hostcache.HostCache

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
class UltrapeerConnectionManager extends ConnectionManager {
	
	final int maxPeers, maxLeafs
	
	final Map<Destination, PeerConnection> peerConnections = new ConcurrentHashMap()
	final Map<Destination, LeafConnection> leafConnections = new ConcurrentHashMap()
	
	UltrapeerConnectionManager() {}

	public UltrapeerConnectionManager(EventBus eventBus, int maxPeers, int maxLeafs, HostCache hostCache) {
		super(eventBus, hostCache)
		this.maxPeers = maxPeers
		this.maxLeafs = maxLeafs
	}
	@Override
	public void drop(Destination d) {
		// TODO Auto-generated method stub
		
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
			new LeafConnection(eventBus, e.endpoint, hostCache) : 
			new PeerConnection(eventBus, e.endpoint, e.incoming, hostCache)
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
}
