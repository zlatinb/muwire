package com.muwire.core.hostcache

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.connection.ConnectionManager

import groovy.json.JsonOutput
import groovy.util.logging.Log
import net.i2p.client.I2PSession
import net.i2p.client.I2PSessionMuxedListener
import net.i2p.client.SendMessageOptions
import net.i2p.client.datagram.I2PDatagramMaker

@Log
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
		
		log.info "Will query hostcaches"	
		
		def ping = [type: "Ping", version: 1, leaf: settings.isLeaf()]
		ping = JsonOutput.toJson(ping)
		def maker = new I2PDatagramMaker(session)
		ping = maker.makeI2PDatagram(ping.bytes)
		def options = new SendMessageOptions()
		options.setSendLeaseSet(true)
		CacheServers.getCacheServers().each {
			log.info "Querying hostcache ${it.toBase32()}"
			session.sendMessage(it, ping, 0, ping.length, I2PSession.PROTO_DATAGRAM, 0, 0, options)
		}
	}
	
	class Listener implements I2PSessionMuxedListener {

		@Override
		public void messageAvailable(I2PSession session, int msgId, long size) {
		}

		@Override
		public void messageAvailable(I2PSession session, int msgId, long size, int proto, int fromport, int toport) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void reportAbuse(I2PSession session, int severity) {
		}

		@Override
		public void disconnected(I2PSession session) {
			log.severe "I2P session disconnected"
		}

		@Override
		public void errorOccurred(I2PSession session, String message, Throwable error) {
			log.severe "I2P error occured $message $error"
		}
		
	}

}
