package com.muwire.core.connection

import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.EventBus
import com.muwire.core.Persona
import com.muwire.core.hostcache.HostCache
import com.muwire.core.search.QueryEvent

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
class LeafConnectionManager extends ConnectionManager {
	
	final int maxConnections
	
	final Map<Destination, UltrapeerConnection> connections = new ConcurrentHashMap()
	
	public LeafConnectionManager(EventBus eventBus, Persona me, int maxConnections, HostCache hostCache) {
		super(eventBus, me, hostCache)
		this.maxConnections = maxConnections
	}
	
	@Override
	public void drop(Destination d) {
		// TODO Auto-generated method stub
		
	}
    
    void onQueryEvent(QueryEvent e) {
        if (me.destination == e.receivedOn) {
            connections.values().each { it.sendQuery(e) }
        }
            
    }

	@Override
	public Collection<Connection> getConnections() {
		connections.values()
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
		if (e.status != ConnectionAttemptStatus.SUCCESSFUL)
			return
			
		Connection c = new UltrapeerConnection(eventBus, e.endpoint)
		connections.put(e.endpoint.destination, c)
		c.start()
	}
	
	@Override 
	public void onDisconnectionEvent(DisconnectionEvent e) {
		def removed = connections.remove(e.destination)
		if (removed == null)
			log.severe("removed destination not present in connection manager ${e.destination.toBase32()}")
	}
	
}
