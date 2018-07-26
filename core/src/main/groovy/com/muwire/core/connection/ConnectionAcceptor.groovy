package com.muwire.core.connection

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.hostcache.HostCache

class ConnectionAcceptor {

	final EventBus eventBus
	final UltrapeerConnectionManager manager
	final MuWireSettings settings
	final I2PAcceptor acceptor
	final HostCache hostCache
	
	final ExecutorService acceptorThread
	final ExecutorService handshakerThreads
	
	ConnectionAcceptor(EventBus eventBus, UltrapeerConnectionManager manager,
		MuWireSettings settings, I2PAcceptor acceptor, HostCache hostCache) {
		this.eventBus = eventBus
		this.manager = manager
		this.settings = settings
		this.acceptor = acceptor
		this.hostCache = hostCache
		
		acceptorThread = Executors.newSingleThreadExecutor { r -> 
			def rv = new Thread(r)
			rv.setDaemon(true)
			rv.setName("acceptor")
			rv
		}
		
		handshakerThreads = Executors.newCachedThreadPool { r ->
			def rv = new Thread(r)
			rv.setDaemon(true)
			rv.setName("acceptor-processor-${System.currentTimeMillis()}")
			rv
		}
	}
	
	void start() {
		acceptorThread.execute({acceptLoop()} as Runnable)
	}
	
	void stop() {
		acceptorThread.shutdownNow()
		handshakerThreads.shutdownNow()
	}
	
	private void acceptLoop() {
		while(true) {
			def incoming = acceptor.accept()
			handshakerThreads.execute({processIncoming(incoming)} as Runnable)
		}
	}
	
	private void processIncoming(Endpoint e) {
		
	}
	
}
