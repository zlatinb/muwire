package com.muwire.core

import com.muwire.core.connection.ConnectionAcceptor
import com.muwire.core.connection.ConnectionEstablisher
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.connection.ConnectionManager
import com.muwire.core.connection.I2PAcceptor
import com.muwire.core.connection.I2PConnector
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
import net.i2p.client.streaming.I2PSocketManager
import net.i2p.client.streaming.I2PSocketManagerFactory

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
		
		
		
		log.info("initializing I2P socket manager")
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
		I2PSocketManager socketManager
		keyDat.withInputStream {
			socketManager = new I2PSocketManagerFactory().createManager(it, sysProps)
		}
		i2pSession = socketManager.getSession()
		
		
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
			new LeafConnectionManager(eventBus,3, hostCache) : new UltrapeerConnectionManager(eventBus, 512, 512, hostCache)
		eventBus.register(TrustEvent.class, connectionManager)
		eventBus.register(ConnectionEvent.class, connectionManager)
		connectionManager.start()
		
		log.info("initializing cache client")
		CacheClient cacheClient = new CacheClient(eventBus,hostCache, connectionManager, i2pSession, props, 10000)
		cacheClient.start()
		
		log.info("initializing acceptor")
		I2PAcceptor i2pAcceptor = new I2PAcceptor(socketManager)
		ConnectionAcceptor acceptor = new ConnectionAcceptor(eventBus, connectionManager, props, i2pAcceptor, hostCache, trustService)
		acceptor.start()
		
		log.info("initializing connector")
		I2PConnector i2pConnector = new I2PConnector(socketManager)
		ConnectionEstablisher connector = new ConnectionEstablisher(eventBus, i2pConnector, props, connectionManager, hostCache)
		connector.start()
		
		// ... at the end, sleep
		log.info("initialized everything, sleeping")
		Thread.sleep(Integer.MAX_VALUE)
	}

}
