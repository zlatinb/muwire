package com.muwire.core.connection

import com.muwire.core.EventBus

import net.i2p.data.Destination

abstract class Connection {

	final EventBus eventBus
	final InputStream inputStream
	final OutputStream outputStream
	final Destination remoteSide
	final boolean incoming
	
	Connection(EventBus eventBus, InputStream inputStream, OutputStream outputStream,
		Destination remoteSide, boolean incoming) {
		this.eventBus = eventBus
		this.inputStream = inputStream
		this.outputStream = outputStream
		this.remoteSide = remoteSide
		this.incoming = incoming
	}
	
	/**
	 * starts the connection threads
	 */
	void start() {
		
	}
}
