package com.muwire.core.connection

import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.EventBus

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
class LeafConnectionManager extends ConnectionManager {
	
	final int maxConnections
	
	final Map<Destination, UltrapeerConnection> connections = new ConcurrentHashMap()

	public LeafConnectionManager(EventBus eventBus, int maxConnections) {
		super(eventBus)
		this.maxConnections = maxConnections
	}
	
	@Override
	public void drop(Destination d) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<Connection> getConnections() {
		// TODO implement
		[]
	}

	@Override
	protected int getDesiredConnections() {
		return maxConnections;
	}

	@Override
	public boolean isConnected(Destination d) {
		connections.containsKey(d)
	}

	@Override
	public void onConnectionEvent(ConnectionEvent e) {
		if (e.incoming || e.leaf) {
			log.severe("Got inconsistent event as a leaf! $e")
			return
		}
		Connection c = new UltrapeerConnection(eventBus, e.endpoint)
		// TODO: start and stuff
		connections.put(e.endpoint.destination, c)
	}
	
}
