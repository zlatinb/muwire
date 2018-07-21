package com.muwire.core.connection

import com.muwire.core.EventBus

class UltrapeerConnectionManager extends ConnectionManager {
	
	final int maxPeers, maxLeafs

	public UltrapeerConnectionManager(EventBus eventBus, int maxPeers, int maxLeafs) {
		super(eventBus)
		this.maxPeers = maxPeers
		this.maxLeafs = maxLeafs
	}

}
