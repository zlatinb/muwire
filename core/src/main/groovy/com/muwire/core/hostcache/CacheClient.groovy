package com.muwire.core.hostcache

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.connection.ConnectionManager

import net.i2p.client.I2PSession

class CacheClient {
	
	final EventBus eventBus
	final HostCache cache
	final ConnectionManager manager
	final I2PSession session
	final long interval
	final MuWireSettings settings
	final Timer timer

	public CacheClient(EventBus eventBus, HostCache cache, 
		ConnectionManager manager, I2PSession session,
		MuWireSettings settings, long interval) {
		this.eventBus = eventBus
		this.cache = cache
		this.manager = manager
		this.session = session
		this.settings = settings
		this.timer = new Timer("hostcache-client",true)
	}
	
	void start() {
		timer.schedule({queryIfNeeded()} as TimerTask, 1, interval)
	}
	
	void stop() {
		timer.cancel()
	}
	
	private void queryIfNeeded() {
		if (manager.hasConnection())
			return
		if (!cache.getHosts(1).isEmpty())
			return
		
		
	}

}
