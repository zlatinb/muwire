package com.muwire.core.connection

import com.muwire.core.EventBus

import net.i2p.data.Destination

abstract class Connection {

	final EventBus eventBus
	final Endpoint endpoint
	final boolean incoming
	
	Connection(EventBus eventBus, Endpoint endpoint, boolean incoming) {
		this.eventBus = eventBus
		this.incoming = incoming
		this.endpoint = endpoint
	}
	
	/**
	 * starts the connection threads
	 */
	void start() {
		
	}
}
