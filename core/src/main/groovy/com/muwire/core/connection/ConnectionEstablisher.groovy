package com.muwire.core.connection

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.hostcache.HostCache

class ConnectionEstablisher {

	final EventBus eventBus
	final I2PConnector i2pConnector
	final MuWireSettings settings
	final ConnectionManager connectionManager
	final HostCache hostCache
	
	final Timer timer
	final ExecutorService executor
	
	ConnectionEstablisher(EventBus eventBus, I2PConnector i2pConnector, MuWireSettings settings,
		ConnectionManager connectionManager, HostCache hostCache) {
		this.eventBus = eventBus
		this.i2pConnector = i2pConnector
		this.settings = settings
		this.connectionManager = connectionManager
		this.hostCache = hostCache
		timer = new Timer("connection-timer",true)
		executor = Executors.newFixedThreadPool(4, { r -> 
			def rv = new Thread(r, true)
			rv.setName("connector-${System.currentTimeMillis()}")
			rv 
		} as ThreadFactory)
	}
	
	void start() {
		timer.schedule({connectIfNeeded()} as TimerTask, 100, 1000)
	}
	
	void stop() {
		timer.cancel()
		executor.shutdownNow()
	}
	
	private void connectIfNeeded() {
		if (!connectionManager.needsConnections())
			return
		
	}
}
