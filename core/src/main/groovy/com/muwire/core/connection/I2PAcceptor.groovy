package com.muwire.core.connection

import net.i2p.client.streaming.I2PSocketManager

class I2PAcceptor {

	final I2PSocketManager socketManager
	
	I2PAcceptor() {}
	
	I2PAcceptor(I2PSocketManager socketManager) {
		this.socketManager = socketManager
	}
	
	Endpoint accept() {
		// TODO implement
		null
	}
}
