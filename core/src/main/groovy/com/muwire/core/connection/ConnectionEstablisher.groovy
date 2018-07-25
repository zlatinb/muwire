package com.muwire.core.connection

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.hostcache.HostCache

import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

class ConnectionEstablisher {
	
	private static final int CONCURRENT = 4

	final EventBus eventBus
	final I2PConnector i2pConnector
	final MuWireSettings settings
	final ConnectionManager connectionManager
	final HostCache hostCache
	
	final Timer timer
	final ExecutorService executor
	
	final Set inProgress = new ConcurrentHashSet()
	
	ConnectionEstablisher(EventBus eventBus, I2PConnector i2pConnector, MuWireSettings settings,
		ConnectionManager connectionManager, HostCache hostCache) {
		this.eventBus = eventBus
		this.i2pConnector = i2pConnector
		this.settings = settings
		this.connectionManager = connectionManager
		this.hostCache = hostCache
		timer = new Timer("connection-timer",true)
		executor = Executors.newFixedThreadPool(CONCURRENT, { r -> 
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
		if (inProgress.size() >= CONCURRENT)
			return

		def toTry
		for (int i = 0; i < 5; i++) {
			toTry = hostCache.getHosts(1)
			if (toTry.isEmpty())
				return
			toTry = toTry[0]
			if (!connectionManager.isConnected(toTry) &&
				!inProgress.contains(toTry)) {
				break
			}
		}
		
		inProgress.add(toTry)
		executor.execute({connect(toTry)} as Runnable)
	}
	
	private void connect(Destination toTry) {
		// TODO: implement
	}
}
