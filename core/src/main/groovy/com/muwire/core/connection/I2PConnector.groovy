package com.muwire.core.connection

import net.i2p.client.streaming.I2PSocketManager
import net.i2p.data.Destination

class I2PConnector {
	
	final I2PSocketManager socketManager
	
	I2PConnector() {}
	
	I2PConnector(I2PSocketManager socketManager) {
		this.socketManager = socketManager
	}
	
	Endpoint connect(Destination dest) {
		def socket = socketManager.connect(dest)
		new Endpoint(dest, socket.getInputStream(), socket.getOutputStream())
	}

}
