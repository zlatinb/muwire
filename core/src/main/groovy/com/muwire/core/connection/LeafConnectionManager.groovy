package com.muwire.core.connection

import com.muwire.core.EventBus

class LeafConnectionManager extends ConnectionManager {
	
	final int maxConnections

	public LeafConnectionManager(EventBus eventBus, int maxConnections) {
		super(eventBus)
		this.maxConnections = maxConnections
	}

}
