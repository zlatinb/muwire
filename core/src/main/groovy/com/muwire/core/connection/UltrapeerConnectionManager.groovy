package com.muwire.core.connection

import java.util.Collection

import com.muwire.core.EventBus

import net.i2p.data.Destination

class UltrapeerConnectionManager extends ConnectionManager {
	
	final int maxPeers, maxLeafs

	public UltrapeerConnectionManager(EventBus eventBus, int maxPeers, int maxLeafs) {
		super(eventBus)
		this.maxPeers = maxPeers
		this.maxLeafs = maxLeafs
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
	
	boolean hasLeafSlots() {
		// TODO implement
		true
	}
	
	boolean hasPeerSlots() {
		// TODO implement
		true
	}
	@Override
	protected int getDesiredConnections() {
		return maxPeers / 2;
	}
	@Override
	public boolean isConnected(Destination d) {
		// TODO Auto-generated method stub
		return false;
	}
}
