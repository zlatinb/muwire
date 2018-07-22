package com.muwire.core.connection

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

}
