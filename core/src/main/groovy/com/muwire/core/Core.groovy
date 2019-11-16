package com.muwire.core

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

import com.muwire.core.chat.ChatDisconnectionEvent
import com.muwire.core.chat.ChatManager
import com.muwire.core.chat.ChatMessageEvent
import com.muwire.core.chat.ChatServer
import com.muwire.core.chat.UIConnectChatEvent
import com.muwire.core.chat.UIDisconnectChatEvent
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
import com.muwire.core.download.SourceDiscoveredEvent
import com.muwire.core.download.UIDownloadCancelledEvent
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.download.UIDownloadPausedEvent
import com.muwire.core.download.UIDownloadResumedEvent
import com.muwire.core.filecert.CertificateClient
import com.muwire.core.filecert.CertificateManager
import com.muwire.core.filecert.UICreateCertificateEvent
import com.muwire.core.filecert.UIFetchCertificatesEvent
import com.muwire.core.filecert.UIImportCertificateEvent
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileHashingEvent
import com.muwire.core.files.FileHasher
import com.muwire.core.files.FileLoadedEvent
import com.muwire.core.files.FileManager
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.files.HasherService
import com.muwire.core.files.PersisterService
import com.muwire.core.files.SideCarFileEvent
import com.muwire.core.files.UICommentEvent
import com.muwire.core.files.UIPersistFilesEvent
import com.muwire.core.files.AllFilesLoadedEvent
import com.muwire.core.files.DirectoryUnsharedEvent
import com.muwire.core.files.DirectoryWatchedEvent
import com.muwire.core.files.DirectoryWatcher
import com.muwire.core.hostcache.CacheClient
import com.muwire.core.hostcache.HostCache
import com.muwire.core.hostcache.HostDiscoveredEvent
import com.muwire.core.mesh.MeshManager
import com.muwire.core.search.BrowseManager
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.ResultsEvent
import com.muwire.core.search.ResultsSender
import com.muwire.core.search.SearchEvent
import com.muwire.core.search.SearchManager
import com.muwire.core.search.UIBrowseEvent
import com.muwire.core.search.UIResultBatchEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustService
import com.muwire.core.trust.TrustSubscriber
import com.muwire.core.trust.TrustSubscriptionEvent
import com.muwire.core.update.UpdateClient
import com.muwire.core.upload.UploadManager
import com.muwire.core.util.MuWireLogManager
import com.muwire.core.content.ContentControlEvent
import com.muwire.core.content.ContentManager

import groovy.util.logging.Log
import net.i2p.I2PAppContext
import net.i2p.client.I2PClientFactory
import net.i2p.client.I2PSession
import net.i2p.client.streaming.I2PSocketManager
import net.i2p.client.streaming.I2PSocketManagerFactory
import net.i2p.client.streaming.I2PSocketOptions
import net.i2p.client.streaming.I2PSocketManager.DisconnectListener
import net.i2p.crypto.DSAEngine
import net.i2p.crypto.SigType
import net.i2p.data.Destination
import net.i2p.data.PrivateKey
import net.i2p.data.Signature
import net.i2p.data.SigningPrivateKey

import net.i2p.router.Router
import net.i2p.router.RouterContext

@Log
public class Core {

    final EventBus eventBus
    final Persona me
    final File home
    final Properties i2pOptions
    final MuWireSettings muOptions

    private final TrustService trustService
    private final TrustSubscriber trustSubscriber
    private final PersisterService persisterService
    private final HostCache hostCache
    private final ConnectionManager connectionManager
    private final CacheClient cacheClient
    private final UpdateClient updateClient
    private final ConnectionAcceptor connectionAcceptor
    private final ConnectionEstablisher connectionEstablisher
    private final HasherService hasherService
    private final DownloadManager downloadManager
    private final DirectoryWatcher directoryWatcher
    final FileManager fileManager
    final UploadManager uploadManager
    final ContentManager contentManager
    final CertificateManager certificateManager
    final ChatServer chatServer
    final ChatManager chatManager

    private final Router router

    final AtomicBoolean shutdown = new AtomicBoolean()
    
    final SigningPrivateKey spk

    public Core(MuWireSettings props, File home, String myVersion) {
        this.home = home
        this.muOptions = props

        i2pOptions = new Properties()
        def i2pOptionsFile = new File(home,"i2p.properties")
        if (i2pOptionsFile.exists()) {
            i2pOptionsFile.withInputStream { i2pOptions.load(it) }

            if (!i2pOptions.containsKey("inbound.nickname"))
                i2pOptions["inbound.nickname"] = "MuWire"
                if (!i2pOptions.containsKey("outbound.nickname"))
                    i2pOptions["outbound.nickname"] = "MuWire"
        } else {
            i2pOptions["inbound.nickname"] = "MuWire"
            i2pOptions["outbound.nickname"] = "MuWire"
            i2pOptions["inbound.length"] = "3"
            i2pOptions["inbound.quantity"] = "4"
            i2pOptions["outbound.length"] = "3"
            i2pOptions["outbound.quantity"] = "4"
            i2pOptions["i2cp.tcp.host"] = "127.0.0.1"
            i2pOptions["i2cp.tcp.port"] = "7654"
            Random r = new Random()
            int port = r.nextInt(60000) + 4000
            i2pOptions["i2np.ntcp.port"] = String.valueOf(port)
            i2pOptions["i2np.udp.port"] = String.valueOf(port)
            i2pOptionsFile.withOutputStream { i2pOptions.store(it, "") }
        }

        if (!props.embeddedRouter) {
            log.info "Initializing I2P context"
            I2PAppContext.getGlobalContext().logManager()
            I2PAppContext.getGlobalContext()._logManager = new MuWireLogManager()
            router = null
        } else {
            log.info("launching embedded router")
            Properties routerProps = new Properties()
            routerProps.setProperty("i2p.dir.base", home.getAbsolutePath())
            routerProps.setProperty("i2p.dir.config", home.getAbsolutePath())
            routerProps.setProperty("router.excludePeerCaps", "KLM")
            routerProps.setProperty("i2np.inboundKBytesPerSecond", String.valueOf(props.inBw))
            routerProps.setProperty("i2np.outboundKBytesPerSecond", String.valueOf(props.outBw))
            routerProps.setProperty("i2cp.disableInterface", "true")
            routerProps.setProperty("i2np.ntcp.port", i2pOptions["i2np.ntcp.port"])
            routerProps.setProperty("i2np.udp.port", i2pOptions["i2np.udp.port"])
            routerProps.setProperty("i2np.udp.internalPort", i2pOptions["i2np.udp.port"])
            router = new Router(routerProps)
            router.getContext().setLogManager(new MuWireLogManager())
            router.runRouter()
            while(!router.isRunning())
                Thread.sleep(100)
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


        // options like tunnel length and quantity
        I2PSession i2pSession
        I2PSocketManager socketManager
        keyDat.withInputStream {
            socketManager = new I2PSocketManagerFactory().createManager(it, i2pOptions["i2cp.tcp.host"], i2pOptions["i2cp.tcp.port"].toInteger(), i2pOptions)
        }
        socketManager.getDefaultOptions().setReadTimeout(60000)
        socketManager.getDefaultOptions().setConnectTimeout(30000)
        socketManager.addDisconnectListener({eventBus.publish(new RouterDisconnectedEvent())} as DisconnectListener)
        i2pSession = socketManager.getSession()

        def destination = new Destination()
        spk = new SigningPrivateKey(Constants.SIG_TYPE)
        keyDat.withInputStream {
            destination.readBytes(it)
            def privateKey = new PrivateKey()
            privateKey.readBytes(it)
            spk.readBytes(it)
        }

        def baos = new ByteArrayOutputStream()
        def daos = new DataOutputStream(baos)
        daos.write(Constants.PERSONA_VERSION)
        byte [] name = props.getNickname().getBytes(StandardCharsets.UTF_8)
        daos.writeShort((short)name.length)
        daos.write(name)
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

        log.info("initializing certificate manager")
        certificateManager = new CertificateManager(eventBus, home, me, spk)
        eventBus.register(UICreateCertificateEvent.class, certificateManager)
        eventBus.register(UIImportCertificateEvent.class, certificateManager)
        
        
        log.info("initializing trust service")
        File goodTrust = new File(home, "trusted")
        File badTrust = new File(home, "distrusted")
        trustService = new TrustService(goodTrust, badTrust, 5000)
        eventBus.register(TrustEvent.class, trustService)


        log.info "initializing file manager"
        fileManager = new FileManager(eventBus, props)
        eventBus.register(FileHashedEvent.class, fileManager)
        eventBus.register(FileLoadedEvent.class, fileManager)
        eventBus.register(FileDownloadedEvent.class, fileManager)
        eventBus.register(FileUnsharedEvent.class, fileManager)
        eventBus.register(SearchEvent.class, fileManager)
        eventBus.register(DirectoryUnsharedEvent.class, fileManager)
        eventBus.register(UICommentEvent.class, fileManager)
        eventBus.register(SideCarFileEvent.class, fileManager)

        log.info("initializing mesh manager")
        MeshManager meshManager = new MeshManager(fileManager, home, props)
        eventBus.register(SourceDiscoveredEvent.class, meshManager)

        log.info "initializing persistence service"
        persisterService = new PersisterService(new File(home, "files.json"), eventBus, 60000, fileManager)
        eventBus.register(UILoadedEvent.class, persisterService)
        eventBus.register(UIPersistFilesEvent.class, persisterService)

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
        updateClient = new UpdateClient(eventBus, i2pSession, myVersion, props, fileManager, me, spk)
        eventBus.register(FileDownloadedEvent.class, updateClient)
        eventBus.register(UIResultBatchEvent.class, updateClient)

        log.info("initializing connector")
        I2PConnector i2pConnector = new I2PConnector(socketManager)

        log.info("initializing certificate client")
        CertificateClient certificateClient = new CertificateClient(eventBus, i2pConnector)
        eventBus.register(UIFetchCertificatesEvent.class, certificateClient)
        
        log.info("initializing chat server")
        chatServer = new ChatServer(eventBus, props, trustService, me, spk)
        eventBus.with {
            register(ChatMessageEvent.class, chatServer)
            register(ChatDisconnectionEvent.class, chatServer)
            register(TrustEvent.class, chatServer)
        }
        
        log.info "initializing results sender"
        ResultsSender resultsSender = new ResultsSender(eventBus, i2pConnector, me, props, certificateManager, chatServer)

        log.info "initializing search manager"
        SearchManager searchManager = new SearchManager(eventBus, me, resultsSender)
        eventBus.register(QueryEvent.class, searchManager)
        eventBus.register(ResultsEvent.class, searchManager)

        log.info("initializing download manager")
        downloadManager = new DownloadManager(eventBus, trustService, meshManager, props, i2pConnector, home, me)
        eventBus.register(UIDownloadEvent.class, downloadManager)
        eventBus.register(UILoadedEvent.class, downloadManager)
        eventBus.register(FileDownloadedEvent.class, downloadManager)
        eventBus.register(UIDownloadCancelledEvent.class, downloadManager)
        eventBus.register(SourceDiscoveredEvent.class, downloadManager)
        eventBus.register(UIDownloadPausedEvent.class, downloadManager)
        eventBus.register(UIDownloadResumedEvent.class, downloadManager)

        log.info("initializing upload manager")
        uploadManager = new UploadManager(eventBus, fileManager, meshManager, downloadManager, props)

        log.info("initializing connection establisher")
        connectionEstablisher = new ConnectionEstablisher(eventBus, i2pConnector, props, connectionManager, hostCache)

        
        log.info("initializing chat manager")
        chatManager = new ChatManager(eventBus, me, i2pConnector, trustService, props)
        eventBus.with { 
            register(UIConnectChatEvent.class, chatManager)
            register(UIDisconnectChatEvent.class, chatManager)
            register(ChatMessageEvent.class, chatManager)
            register(ChatDisconnectionEvent.class, chatManager)
        }
        
        log.info("initializing acceptor")
        I2PAcceptor i2pAcceptor = new I2PAcceptor(socketManager)
        connectionAcceptor = new ConnectionAcceptor(eventBus, connectionManager, props,
            i2pAcceptor, hostCache, trustService, searchManager, uploadManager, fileManager, connectionEstablisher,
            certificateManager, chatServer)

        log.info("initializing directory watcher")
        directoryWatcher = new DirectoryWatcher(eventBus, fileManager, home, props)
        eventBus.register(DirectoryWatchedEvent.class, directoryWatcher)
        eventBus.register(AllFilesLoadedEvent.class, directoryWatcher)
        eventBus.register(DirectoryUnsharedEvent.class, directoryWatcher)

        log.info("initializing hasher service")
        hasherService = new HasherService(new FileHasher(), eventBus, fileManager, props)
        eventBus.register(FileSharedEvent.class, hasherService)
        eventBus.register(FileUnsharedEvent.class, hasherService)
        eventBus.register(DirectoryUnsharedEvent.class, hasherService)

        log.info("initializing trust subscriber")
        trustSubscriber = new TrustSubscriber(eventBus, i2pConnector, props)
        eventBus.register(UILoadedEvent.class, trustSubscriber)
        eventBus.register(TrustSubscriptionEvent.class, trustSubscriber)
        
        log.info("initializing content manager")
        contentManager = new ContentManager()
        eventBus.register(ContentControlEvent.class, contentManager)
        eventBus.register(QueryEvent.class, contentManager)
        
        log.info("initializing browse manager")
        BrowseManager browseManager = new BrowseManager(i2pConnector, eventBus, me)
        eventBus.register(UIBrowseEvent.class, browseManager)
        
    }

    public void startServices() {
        hasherService.start()
        trustService.start()
        trustService.waitForLoad()
        hostCache.start()
        connectionManager.start()
        cacheClient.start()
        connectionAcceptor.start()
        connectionEstablisher.start()
        hostCache.waitForLoad()
        updateClient.start()
    }

    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            log.info("already shutting down")
            return
        }
        log.info("saving settings")
        saveMuSettings()
        log.info("shutting down trust subscriber")
        trustSubscriber.stop()
        log.info("shutting down download manager")
        downloadManager.shutdown()
        log.info("shutting down connection acceptor")
        connectionAcceptor.stop()
        log.info("shutting down connection establisher")
        connectionEstablisher.stop()
        log.info("shutting down directory watcher")
        directoryWatcher.stop()
        log.info("shutting down cache client")
        cacheClient.stop()
        log.info("shutting down chat server")
        chatServer.stop()
        log.info("shutting down chat manager")
        chatManager.shutdown()
        log.info("shutting down connection manager")
        connectionManager.shutdown()
        if (router != null) {
            log.info("shutting down embedded router")
            router.shutdown(0)
        }
        log.info("shutdown complete")
    }
    
    public void saveMuSettings() {
        File f = new File(home, "MuWire.properties")
        f.withPrintWriter("UTF-8", { muOptions.write(it) })
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

        Core core = new Core(props, home, "0.6.6")
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
