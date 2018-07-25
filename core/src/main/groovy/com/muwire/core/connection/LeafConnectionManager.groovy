package com.muwire.core.connection

import com.muwire.core.EventBus

import net.i2p.data.Destination

class LeafConnectionManager extends ConnectionManager {
	
	final int maxConnections

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
	
	
}
