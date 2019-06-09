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
import com.muwire.core.download.DownloadManager
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileHasher
import com.muwire.core.files.FileLoadedEvent
import com.muwire.core.files.FileManager
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.files.HasherService
import com.muwire.core.files.PersisterService
import com.muwire.core.hostcache.CacheClient
import com.muwire.core.hostcache.HostCache
import com.muwire.core.hostcache.HostDiscoveredEvent
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.ResultsEvent
import com.muwire.core.search.ResultsSender
import com.muwire.core.search.SearchEvent
import com.muwire.core.search.SearchManager
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustService
import com.muwire.core.update.UpdateClient
import com.muwire.core.upload.UploadManager
import com.muwire.core.util.MuWireLogManager

import groovy.util.logging.Log
import net.i2p.I2PAppContext
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
public class Core {
    
    final EventBus eventBus
    final Persona me
    final File home
    final Properties i2pOptions
    final MuWireSettings muOptions
    
    private final TrustService trustService
    private final PersisterService persisterService
    private final HostCache hostCache
    private final ConnectionManager connectionManager
    private final CacheClient cacheClient
    private final UpdateClient updateClient
    private final ConnectionAcceptor connectionAcceptor
    private final ConnectionEstablisher connectionEstablisher
    private final HasherService hasherService
        
    public Core(MuWireSettings props, File home, String myVersion) {
        this.home = home		
        this.muOptions = props
        log.info "Initializing I2P context"
        I2PAppContext.getGlobalContext().logManager()
        I2PAppContext.getGlobalContext()._logManager = new MuWireLogManager()
        
		log.info("initializing I2P socket manager")
		def i2pClient = new I2PClientFactory().createClient()
		File keyDat = new File(home, "key.dat")
		if (!keyDat.exists()) {
			log.info("Creating new key.dat")
			keyDat.withOutputStream { 
				i2pClient.createDestination(it, Constants.SIG_TYPE)
			}
		}
		
        i2pOptions = new Properties()
        def i2pOptionsFile = new File(home,"i2p.properties")
        if (i2pOptionsFile.exists()) {
            i2pOptionsFile.withInputStream { i2pOptions.load(it) }
            if (!i2pOptions.hasProperty("inbound.nickname"))
                i2pOptions["inbound.nickname"] = "MuWire"
            if (!i2pOptions.hasProperty("outbound.nickname"))
                i2pOptions["outbound.nickname"] = "MuWire"
        } else {
            i2pOptions["inbound.nickname"] = "MuWire"
            i2pOptions["outbound.nickname"] = "MuWire"
            i2pOptions["inbound.length"] = "3"
            i2pOptions["inbound.quantity"] = "2"
            i2pOptions["outbound.length"] = "3"
            i2pOptions["outbound.quantity"] = "2"
        }
        
        // options like tunnel length and quantity
		I2PSession i2pSession
		I2PSocketManager socketManager
		keyDat.withInputStream {
			socketManager = new I2PSocketManagerFactory().createManager(it, i2pOptions)
		}
		socketManager.getDefaultOptions().setReadTimeout(60000)
		socketManager.getDefaultOptions().setConnectTimeout(30000)
		i2pSession = socketManager.getSession()
		
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

		eventBus = new EventBus()
		
		log.info("initializing trust service")
		File goodTrust = new File(home, "trusted")
		File badTrust = new File(home, "distrusted")
		trustService = new TrustService(goodTrust, badTrust, 5000)
		eventBus.register(TrustEvent.class, trustService)
		
		
		log.info "initializing file manager"
		FileManager fileManager = new FileManager(eventBus, props)
		eventBus.register(FileHashedEvent.class, fileManager)
		eventBus.register(FileLoadedEvent.class, fileManager)
		eventBus.register(FileDownloadedEvent.class, fileManager)
		eventBus.register(FileUnsharedEvent.class, fileManager)
		eventBus.register(SearchEvent.class, fileManager)
		
		log.info "initializing persistence service"
		persisterService = new PersisterService(new File(home, "files.json"), eventBus, 5000, fileManager)
        
		log.info("initializing host cache")
		File hostStorage = new File(home, "hosts.json")
        hostCache = new HostCache(trustService,hostStorage, 30000, props, i2pSession.getMyDestination())
		eventBus.register(HostDiscoveredEvent.class, hostCache)
		eventBus.register(ConnectionEvent.class, hostCache)
		
		log.info("initializing connection manager")
		connectionManager = props.isLeaf() ? 
			new LeafConnectionManager(eventBus, me, 3, hostCache, props) : 
            new UltrapeerConnectionManager(eventBus, me, 512, 512, hostCache, trustService, props)
		eventBus.register(TrustEvent.class, connectionManager)
		eventBus.register(ConnectionEvent.class, connectionManager)
		eventBus.register(DisconnectionEvent.class, connectionManager)
        eventBus.register(QueryEvent.class, connectionManager)
		
		log.info("initializing cache client")
		cacheClient = new CacheClient(eventBus,hostCache, connectionManager, i2pSession, props, 10000)
        
        log.info("initializing update client")
        updateClient = new UpdateClient(eventBus, i2pSession, myVersion, props)
        
		log.info("initializing connector")
		I2PConnector i2pConnector = new I2PConnector(socketManager)
        
		log.info "initializing results sender"
		ResultsSender resultsSender = new ResultsSender(eventBus, i2pConnector, me)
		
		log.info "initializing search manager"
		SearchManager searchManager = new SearchManager(eventBus, me, resultsSender)
		eventBus.register(QueryEvent.class, searchManager)
		eventBus.register(ResultsEvent.class, searchManager)
		
        log.info("initializing download manager")
        DownloadManager downloadManager = new DownloadManager(eventBus, i2pConnector, new File(home, "incompletes"), me)
        eventBus.register(UIDownloadEvent.class, downloadManager)
        eventBus.register(UILoadedEvent.class, downloadManager)
        eventBus.register(FileDownloadedEvent.class, downloadManager)
        
        log.info("initializing upload manager")
        UploadManager uploadManager = new UploadManager(eventBus, fileManager)
        
        log.info("initializing connection establisher")
        connectionEstablisher = new ConnectionEstablisher(eventBus, i2pConnector, props, connectionManager, hostCache)
        
		log.info("initializing acceptor")
		I2PAcceptor i2pAcceptor = new I2PAcceptor(socketManager)
		connectionAcceptor = new ConnectionAcceptor(eventBus, connectionManager, props, 
            i2pAcceptor, hostCache, trustService, searchManager, uploadManager, connectionEstablisher)
		
        
        log.info("initializing hasher service")
        hasherService = new HasherService(new FileHasher(), eventBus, fileManager)
        eventBus.register(FileSharedEvent.class, hasherService)
	}
    
    public void startServices() {
        hasherService.start()
        trustService.start()
        trustService.waitForLoad()
        persisterService.start()
        hostCache.start()
        connectionManager.start()
        cacheClient.start()
        connectionAcceptor.start()
        connectionEstablisher.start()
        hostCache.waitForLoad()
        updateClient.start()
    }
    
    public void shutdown() {
        connectionManager.shutdown()
    }

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
        
        Core core = new Core(props, home, "0.1.3")
        core.startServices()
        
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
            def shell = new GroovyShell(binding)
            binding.setProperty('eventBus', core.eventBus)
            binding.setProperty('me', core.me)
            // TOOD: other bindings?
            def script = shell.parse(f)
            script.run()
        }
    }
}
