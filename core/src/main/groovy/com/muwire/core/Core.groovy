package com.muwire.core

import java.nio.charset.StandardCharsets

import com.muwire.core.connection.ConnectionAcceptor
import com.muwire.core.connection.ConnectionEstablisher
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.connection.ConnectionManager
import com.muwire.core.connection.DisconnectionEvent
import com.muwire.core.connection.I2PAcceptor
import com.muwire.core.connection.I2PConnector
import com.muwire.core.connection.LeafConnectionManager
import com.muwire.core.connection.UltrapeerConnectionManager
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileHasher
import com.muwire.core.files.FileLoadedEvent
import com.muwire.core.files.FileManager
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.files.HasherService
import com.muwire.core.hostcache.CacheClient
import com.muwire.core.hostcache.HostCache
import com.muwire.core.hostcache.HostDiscoveredEvent
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.ResultsEvent
import com.muwire.core.search.ResultsSender
import com.muwire.core.search.SearchManager
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustService

import groovy.util.logging.Log
import net.i2p.client.I2PClientFactory
import net.i2p.client.I2PSession
import net.i2p.client.streaming.I2PSocketManager
import net.i2p.client.streaming.I2PSocketManagerFactory
import net.i2p.client.streaming.I2PSocketOptions
import net.i2p.crypto.DSAEngine
import net.i2p.crypto.SigType
import net.i2p.data.Destination
import net.i2p.data.PrivateKey
import net.i2p.data.Signature
import net.i2p.data.SigningPrivateKey

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
			props = new MuWireSettings(props)
		} else {
			log.info("creating default properties")
            props = new MuWireSettings()
            propsFile.withOutputStream { 
                props.write(it)
            }
		}
		
		log.info("initializing I2P socket manager")
		def i2pClient = new I2PClientFactory().createClient()
		File keyDat = new File(home, "key.dat")
		if (!keyDat.exists()) {
			log.info("Creating new key.dat")
			keyDat.withOutputStream { 
				i2pClient.createDestination(it, Constants.SIG_TYPE)
			}
		}
		
		def sysProps = System.getProperties().clone()
		sysProps["inbound.nickname"] = "MuWire"
		I2PSession i2pSession
		I2PSocketManager socketManager
		keyDat.withInputStream {
			socketManager = new I2PSocketManagerFactory().createManager(it, sysProps)
		}
		socketManager.getDefaultOptions().setReadTimeout(60000)
		socketManager.getDefaultOptions().setConnectTimeout(30000)
		i2pSession = socketManager.getSession()
		
        Persona me
        def destination = new Destination()
        def spk = new SigningPrivateKey(Constants.SIG_TYPE)
        keyDat.withInputStream {
            destination.readBytes(it)
            def privateKey = new PrivateKey()
            privateKey.readBytes(it)
            spk.readBytes(it)
		}
            
        def baos = new ByteArrayOutputStream()
        def daos = new DataOutputStream(baos)
        daos.write(Constants.PERSONA_VERSION)
        daos.writeShort((short)props.getNickname().length())
        daos.write(props.getNickname().getBytes(StandardCharsets.UTF_8))
        destination.writeBytes(daos)
        daos.flush()
        byte [] payload = baos.toByteArray()
        Signature sig = DSAEngine.getInstance().sign(payload, spk)

        baos = new ByteArrayOutputStream()
        baos.write(payload)
        sig.writeBytes(baos)
        me = new Persona(new ByteArrayInputStream(baos.toByteArray()))
        log.info("Loaded myself as "+me.getHumanReadableName())

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
		eventBus.register(DisconnectionEvent.class, connectionManager)
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
        
        log.info("initializing hasher service")
        HasherService hasherService = new HasherService(new FileHasher(), eventBus)
        eventBus.register(FileSharedEvent.class, hasherService)
        hasherService.start()
        
        log.info "initializing file manager"
        FileManager fileManager = new FileManager(eventBus)
        eventBus.register(FileHashedEvent.class, fileManager)
        eventBus.register(FileLoadedEvent.class, fileManager)
        eventBus.register(FileDownloadedEvent.class, fileManager)
        eventBus.register(FileUnsharedEvent.class, fileManager)
        
        log.info "initializing results sender"
        ResultsSender resultsSender = new ResultsSender()
        
        log.info "initializing search manager"
        SearchManager searchManager = new SearchManager(eventBus, resultsSender)
        eventBus.register(QueryEvent.class, searchManager)
        eventBus.register(ResultsEvent.class, searchManager)
		
		// ... at the end, sleep or execute script
		if (args.length == 0) {
			log.info("initialized everything, sleeping")
			Thread.sleep(Integer.MAX_VALUE)
		} else {
			log.info("executing script ${args[0]}")
			File f = new File(args[0])
			if (!f.exists()) {
				log.warning("Script file doesn't exist")
				System.exit(1)
			}
			
			def binding = new Binding()
			binding.setProperty("eventBus", eventBus)
			// TOOD: other bindings?
			def shell = new GroovyShell(binding)
			def script = shell.parse(f)
			script.run()
		}
	}

}
