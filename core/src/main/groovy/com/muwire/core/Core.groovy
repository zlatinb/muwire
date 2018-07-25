package com.muwire.core

import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.connection.ConnectionManager
import com.muwire.core.connection.LeafConnectionManager
import com.muwire.core.connection.UltrapeerConnectionManager
import com.muwire.core.hostcache.CacheClient
import com.muwire.core.hostcache.HostCache
import com.muwire.core.hostcache.HostDiscoveredEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustService

import groovy.util.logging.Log
import net.i2p.client.I2PClientFactory
import net.i2p.client.I2PSession

@Log
class Core {

	static main(args) {
		def home = System.getProperty("user.home") + File.separator + ".MuWire"
		home = new File(home)
		if (!home.exists()) {
			log.info("creating home dir")
			home.mkdir()
		}
		
		def props = new Properties()
		def propsFile = new File(home, "MuWire.properties")
		if (propsFile.exists()) {
			log.info("loading existing props file")
			propsFile.withInputStream {
				props.load(it)
			}
		} else
			log.info("creating default properties")
		
		props = new MuWireSettings(props)
		
		
		
		log.info("initializing I2P session")
		def i2pClient = new I2PClientFactory().createClient()
		File keyDat = new File(home, "key.dat")
		if (!keyDat.exists()) {
			log.info("Creating new key.dat")
			keyDat.withOutputStream { 
				i2pClient.createDestination(it)
			}
		}
		
		def sysProps = System.getProperties().clone()
				sysProps["inbound.nickname"] = "MuWire"
				I2PSession i2pSession
				keyDat.withInputStream { 
			i2pSession = i2pClient.createSession(it, sysProps)
		}
		
		log.info("connecting i2p session")
		i2pSession.connect()
		
		EventBus eventBus = new EventBus()
		
		log.info("initializing trust service")
		File goodTrust = new File(home, "trust.good")
		File badTrust = new File(home, "trust.bad")
		TrustService trustService = new TrustService(goodTrust, badTrust, 5000)
		eventBus.register(TrustEvent.class, trustService)
		trustService.start()
		trustService.waitForLoad()
		
		log.info("initializing host cache")
		File hostStorage = new File(home, "hosts.json")
		HostCache hostCache = new HostCache(trustService,hostStorage, 30000, props, i2pSession.getMyDestination())
		eventBus.register(HostDiscoveredEvent.class, hostCache)
		eventBus.register(ConnectionEvent.class, hostCache)
		hostCache.start()
		hostCache.waitForLoad()
		
		log.info("initializing connection manager")
		ConnectionManager connectionManager = props.isLeaf() ? 
			new LeafConnectionManager(eventBus,3) : new UltrapeerConnectionManager(eventBus, 512, 512)
		eventBus.register(TrustEvent.class, connectionManager)
		
		log.info("initializing cache client")
		CacheClient cacheClient = new CacheClient(eventBus,hostCache, connectionManager, i2pSession, props, 10000)
		cacheClient.start()
		
		// ... at the end, sleep
		log.info("initialized everything, sleeping")
		Thread.sleep(Integer.MAX_VALUE)
	}

}
