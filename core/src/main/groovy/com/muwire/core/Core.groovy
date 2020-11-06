package com.muwire.core

import com.muwire.core.files.PersisterDoneEvent
import com.muwire.core.files.PersisterFolderService

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import java.util.logging.Level
import java.util.zip.ZipException

import com.muwire.core.chat.ChatDisconnectionEvent
import com.muwire.core.chat.ChatManager
import com.muwire.core.chat.ChatMessageEvent
import com.muwire.core.chat.ChatServer
import com.muwire.core.chat.UIConnectChatEvent
import com.muwire.core.chat.UIDisconnectChatEvent
import com.muwire.core.collections.CollectionManager
import com.muwire.core.collections.CollectionsClient
import com.muwire.core.collections.UICollectionCreatedEvent
import com.muwire.core.collections.UICollectionDeletedEvent
import com.muwire.core.collections.UICollectionFetchEvent
import com.muwire.core.collections.UIDownloadCollectionEvent
import com.muwire.core.connection.ConnectionAcceptor
import com.muwire.core.connection.ConnectionEstablisher
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.connection.ConnectionManager
import com.muwire.core.connection.DisconnectionEvent
import com.muwire.core.connection.I2PAcceptor
import com.muwire.core.connection.I2PConnector
import com.muwire.core.connection.LeafConnectionManager
import com.muwire.core.connection.UltrapeerConnectionManager
import com.muwire.core.download.DownloadHopelessEvent
import com.muwire.core.download.DownloadManager
import com.muwire.core.download.SourceDiscoveredEvent
import com.muwire.core.download.SourceVerifiedEvent
import com.muwire.core.download.UIDownloadCancelledEvent
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.download.UIDownloadPausedEvent
import com.muwire.core.download.UIDownloadResumedEvent
import com.muwire.core.filecert.CertificateClient
import com.muwire.core.filecert.CertificateManager
import com.muwire.core.filecert.UICreateCertificateEvent
import com.muwire.core.filecert.UIFetchCertificatesEvent
import com.muwire.core.filecert.UIImportCertificateEvent
import com.muwire.core.filefeeds.FeedClient
import com.muwire.core.filefeeds.FeedFetchEvent
import com.muwire.core.filefeeds.FeedItemFetchedEvent
import com.muwire.core.filefeeds.FeedManager
import com.muwire.core.filefeeds.UIDownloadFeedItemEvent
import com.muwire.core.filefeeds.UIFilePublishedEvent
import com.muwire.core.filefeeds.UIFeedConfigurationEvent
import com.muwire.core.filefeeds.UIFeedDeletedEvent
import com.muwire.core.filefeeds.UIFeedUpdateEvent
import com.muwire.core.filefeeds.UIFileUnpublishedEvent
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileHasher
import com.muwire.core.files.FileLoadedEvent
import com.muwire.core.files.FileManager
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.files.HasherService
import com.muwire.core.files.PersisterService
import com.muwire.core.files.SideCarFileEvent
import com.muwire.core.files.UICommentEvent
import com.muwire.core.files.directories.UISyncDirectoryEvent
import com.muwire.core.files.directories.WatchedDirectoryConfigurationEvent
import com.muwire.core.files.directories.WatchedDirectoryConvertedEvent
import com.muwire.core.files.directories.WatchedDirectoryConverter
import com.muwire.core.files.directories.WatchedDirectoryManager
import com.muwire.core.files.AllFilesLoadedEvent
import com.muwire.core.files.DirectoryUnsharedEvent
import com.muwire.core.files.DirectoryWatchedEvent
import com.muwire.core.files.DirectoryWatcher
import com.muwire.core.hostcache.CacheClient
import com.muwire.core.hostcache.H2HostCache
import com.muwire.core.hostcache.HostCache
import com.muwire.core.hostcache.HostDiscoveredEvent
import com.muwire.core.hostcache.SimpleHostCache
import com.muwire.core.mesh.MeshManager
import com.muwire.core.messenger.MessageReceivedEvent
import com.muwire.core.messenger.Messenger
import com.muwire.core.messenger.UIDownloadAttachmentEvent
import com.muwire.core.messenger.UIMessageDeleteEvent
import com.muwire.core.messenger.UIMessageEvent
import com.muwire.core.messenger.UIMessageReadEvent
import com.muwire.core.search.BrowseManager
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.ResponderCache
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
import com.muwire.core.tracker.TrackerResponder

import groovy.util.logging.Log
import net.i2p.I2PAppContext
import net.i2p.client.I2PClientFactory
import net.i2p.client.I2PSession
import net.i2p.client.streaming.I2PSocketManager
import net.i2p.client.streaming.I2PSocketManagerFactory
import net.i2p.client.streaming.I2PSocketManager.DisconnectListener
import net.i2p.crypto.DSAEngine
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
    final String version;
    final File home
    final Properties i2pOptions
    final MuWireSettings muOptions

    final I2PSession i2pSession;
    private I2PSocketManager i2pSocketManager
    final TrustService trustService
    final TrustSubscriber trustSubscriber
    private final PersisterService persisterService
    private final PersisterFolderService persisterFolderService
    final HostCache hostCache
    final ConnectionManager connectionManager
    private final CacheClient cacheClient
    private final UpdateClient updateClient
    final ConnectionAcceptor connectionAcceptor
    private final ConnectionEstablisher connectionEstablisher
    private final HasherService hasherService
    final DownloadManager downloadManager
    private final DirectoryWatcher directoryWatcher
    final FileManager fileManager
    final CollectionManager collectionManager
    final UploadManager uploadManager
    final ContentManager contentManager
    final CertificateManager certificateManager
    final ChatServer chatServer
    final ChatManager chatManager
    final Messenger messenger
    final FeedManager feedManager
    private final FeedClient feedClient
    private final WatchedDirectoryConverter watchedDirectoryConverter
    final WatchedDirectoryManager watchedDirectoryManager
    private final TrackerResponder trackerResponder

    private final Router router

    final AtomicBoolean shutdown = new AtomicBoolean()
    
    final SigningPrivateKey spk

    public Core(MuWireSettings props, File home, String myVersion) {
        this.home = home
        this.version = myVersion
        this.muOptions = props

        i2pOptions = new Properties()
        // Read defaults
        def defaultI2PFile = getClass()
                .getClassLoader().getResource("defaults/i2p.properties");
        try {
            defaultI2PFile.withInputStream { i2pOptions.load(it) }
        } catch (ZipException mystery) {
            log.log(Level.SEVERE, "couldn't load default i2p properties", mystery)
        }

        def i2pOptionsFile = new File(home, "i2p.properties")
        if (i2pOptionsFile.exists()) {
            i2pOptionsFile.withInputStream { i2pOptions.load(it) }

            if (!i2pOptions.containsKey("inbound.nickname"))
                i2pOptions["inbound.nickname"] = "MuWire"
            if (!i2pOptions.containsKey("outbound.nickname"))
                i2pOptions["outbound.nickname"] = "MuWire"
            if (!i2pOptions.containsKey("router.excludePeerCaps"))
                i2pOptions["router.excludePeerCaps"] = "KLMNO"
        }
        if (!(i2pOptions.containsKey("i2np.ntcp.port")
                && i2pOptions.containsKey("i2np.udp.port")
        )) {
            Random r = new Random()
            int port = 9151 + r.nextInt(1 + 30777 - 9151)  // this range matches what the i2p router would choose
            i2pOptions["i2np.ntcp.port"] = String.valueOf(port)
            i2pOptions["i2np.udp.port"] = String.valueOf(port)
            i2pOptionsFile.withOutputStream { i2pOptions.store(it, "") }
        }
        
        i2pOptions['i2cp.leaseSetEncType']='4,0'

        if (!props.embeddedRouter) {
            if (!(I2PAppContext.getGlobalContext() instanceof RouterContext)) {
                log.info "Initializing I2P context"
                I2PAppContext.getGlobalContext().logManager()
                I2PAppContext.getGlobalContext()._logManager = new MuWireLogManager()
                router = null
            }
        } else {
            log.info("launching embedded router")
            Properties routerProps = new Properties()
            routerProps.setProperty("i2p.dir.base", home.getAbsolutePath())
            routerProps.setProperty("i2p.dir.config", home.getAbsolutePath())
            routerProps.setProperty("geoip.dir", home.getAbsolutePath() + File.separator + "geoip")
            routerProps.setProperty("router.excludePeerCaps", i2pOptions["router.excludePeerCaps"])
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
        keyDat.withInputStream {
            i2pSocketManager = new I2PSocketManagerFactory().createDisconnectedManager(it, i2pOptions["i2cp.tcp.host"], i2pOptions["i2cp.tcp.port"].toInteger(), i2pOptions)
        }
        i2pSocketManager.getDefaultOptions().setReadTimeout(60000)
        i2pSocketManager.getDefaultOptions().setConnectTimeout(30000)
        i2pSocketManager.addDisconnectListener({eventBus.publish(new RouterDisconnectedEvent())} as DisconnectListener)
        i2pSession = i2pSocketManager.getSession()

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
        
        log.info("initializing collection manager")
        collectionManager = new CollectionManager(eventBus, fileManager, home)
        eventBus.with { 
            register(AllFilesLoadedEvent.class, collectionManager)
            register(UICollectionCreatedEvent.class, collectionManager)
            register(UICollectionDeletedEvent.class, collectionManager)
            register(UIDownloadCollectionEvent.class, collectionManager)
            register(FileDownloadedEvent.class, collectionManager)
            register(FileUnsharedEvent.class, collectionManager)
            register(SearchEvent.class, collectionManager)
        }
        
        log.info("initializing mesh manager")
        MeshManager meshManager = new MeshManager(fileManager, home, props)
        eventBus.register(SourceDiscoveredEvent.class, meshManager)
        eventBus.register(SourceVerifiedEvent.class, meshManager)

        log.info "initializing persistence service"
        persisterService = new PersisterService(new File(home, "files.json"), eventBus, 60000, fileManager)
        eventBus.register(UILoadedEvent.class, persisterService)

        log.info "initializing folder persistence service"
        persisterFolderService = new PersisterFolderService(this, new File(home, "files"), eventBus)
        eventBus.register(PersisterDoneEvent.class, persisterFolderService)
        eventBus.register(FileDownloadedEvent.class, persisterFolderService)
        eventBus.register(FileLoadedEvent.class, persisterFolderService)
        eventBus.register(FileHashedEvent.class, persisterFolderService)
        eventBus.register(FileUnsharedEvent.class, persisterFolderService)
        eventBus.register(UICommentEvent.class, persisterFolderService)
        eventBus.register(UIFilePublishedEvent.class, persisterFolderService)
        eventBus.register(UIFileUnpublishedEvent.class, persisterFolderService)

        log.info("initializing host cache")
        hostCache = new H2HostCache(home,trustService, props, i2pSession.getMyDestination())
        eventBus.register(HostDiscoveredEvent.class, hostCache)
        eventBus.register(ConnectionEvent.class, hostCache)

        
        log.info("initializing responder cache")
        ResponderCache responderCache = new ResponderCache(props.responderCacheSize)
        eventBus.register(UIResultBatchEvent.class, responderCache)
        eventBus.register(SourceVerifiedEvent.class, responderCache)
        
        
        log.info("initializing connection manager")
        connectionManager = props.isLeaf() ?
            new LeafConnectionManager(eventBus, me, 3, hostCache, props) :
            new UltrapeerConnectionManager(eventBus, me, props.peerConnections, props.leafConnections, hostCache, responderCache, trustService, props)
        eventBus.register(TrustEvent.class, connectionManager)
        eventBus.register(ConnectionEvent.class, connectionManager)
        eventBus.register(DisconnectionEvent.class, connectionManager)
        eventBus.register(QueryEvent.class, connectionManager)

        log.info("initializing cache client")
        cacheClient = new CacheClient(eventBus,hostCache, connectionManager, i2pSession, props, 10000)

        if (!(props.plugin || props.disableUpdates)) {
        log.info("initializing update client")
            updateClient = new UpdateClient(eventBus, i2pSession, myVersion, props, fileManager, me, spk)
            eventBus.register(FileDownloadedEvent.class, updateClient)
            eventBus.register(UIResultBatchEvent.class, updateClient)
        } else
            log.info("running as plugin or updates disabled, not initializing update client")

        log.info("initializing connector")
        I2PConnector i2pConnector = new I2PConnector(i2pSocketManager)
        
        log.info("initializing collections client")
        CollectionsClient collectionsClient = new CollectionsClient(i2pConnector, eventBus, me)
        eventBus.register(UICollectionFetchEvent.class, collectionsClient)

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
        
        log.info("initializing feed manager")
        feedManager = new FeedManager(eventBus, home)
        eventBus.with { 
            register(FeedItemFetchedEvent.class, feedManager)
            register(FeedFetchEvent.class, feedManager)
            register(UIFeedConfigurationEvent.class, feedManager)
            register(UIFeedDeletedEvent.class, feedManager)
        }
        
        log.info("initializing feed client")
        feedClient = new FeedClient(i2pConnector, eventBus, me, feedManager)
        eventBus.register(UIFeedUpdateEvent.class, feedClient)
        
        log.info "initializing results sender"
        ResultsSender resultsSender = new ResultsSender(eventBus, i2pConnector, me, props, certificateManager, chatServer, collectionManager)

        log.info "initializing search manager"
        SearchManager searchManager = new SearchManager(eventBus, me, resultsSender)
        eventBus.register(QueryEvent.class, searchManager)
        eventBus.register(ResultsEvent.class, searchManager)

        log.info("initializing chat manager")
        chatManager = new ChatManager(eventBus, me, i2pConnector, trustService, props)
        eventBus.with { 
            register(UIConnectChatEvent.class, chatManager)
            register(UIDisconnectChatEvent.class, chatManager)
            register(ChatMessageEvent.class, chatManager)
            register(ChatDisconnectionEvent.class, chatManager)
        }
        
        log.info("initializing download manager")
        downloadManager = new DownloadManager(eventBus, trustService, meshManager, props, i2pConnector, home, me, chatServer)
        eventBus.register(UIDownloadEvent.class, downloadManager)
        eventBus.register(UIDownloadFeedItemEvent.class, downloadManager)
        eventBus.register(UILoadedEvent.class, downloadManager)
        eventBus.register(FileDownloadedEvent.class, downloadManager)
        eventBus.register(UIDownloadCancelledEvent.class, downloadManager)
        eventBus.register(SourceDiscoveredEvent.class, downloadManager)
        eventBus.register(UIDownloadPausedEvent.class, downloadManager)
        eventBus.register(UIDownloadResumedEvent.class, downloadManager)
        eventBus.register(DownloadHopelessEvent.class, downloadManager)
        eventBus.register(UIDownloadCollectionEvent.class, downloadManager)
        eventBus.register(UIDownloadAttachmentEvent.class, downloadManager)

        log.info("initializing upload manager")
        uploadManager = new UploadManager(eventBus, fileManager, meshManager, downloadManager, persisterFolderService, props)
        
        log.info("initializing tracker responder")
        trackerResponder = new TrackerResponder(i2pSession, props, fileManager, downloadManager, meshManager, trustService, me)

        log.info("initializing connection establisher")
        connectionEstablisher = new ConnectionEstablisher(eventBus, i2pConnector, props, connectionManager, hostCache)

        log.info("initializing acceptor")
        I2PAcceptor i2pAcceptor = new I2PAcceptor(i2pSocketManager)
        connectionAcceptor = new ConnectionAcceptor(eventBus, connectionManager, props,
            i2pAcceptor, hostCache, trustService, searchManager, uploadManager, fileManager, connectionEstablisher,
            certificateManager, chatServer, collectionManager)


        log.info("initializing hasher service")
        hasherService = new HasherService(eventBus, fileManager, props)
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
        
        log.info("initializing watched directory converter")
        watchedDirectoryConverter = new WatchedDirectoryConverter(this)
        eventBus.register(AllFilesLoadedEvent.class, watchedDirectoryConverter)
        
        log.info("initializing watched directory manager")
        watchedDirectoryManager = new WatchedDirectoryManager(home, eventBus, fileManager)
        eventBus.with { 
            register(WatchedDirectoryConfigurationEvent.class, watchedDirectoryManager)
            register(WatchedDirectoryConvertedEvent.class, watchedDirectoryManager)
            register(FileSharedEvent.class, watchedDirectoryManager)
            register(DirectoryUnsharedEvent.class, watchedDirectoryManager)
            register(UISyncDirectoryEvent.class, watchedDirectoryManager)
        }
        
        log.info("initializing directory watcher")
        directoryWatcher = new DirectoryWatcher(eventBus, fileManager, home, watchedDirectoryManager)
        eventBus.with {
            register(DirectoryWatchedEvent.class, directoryWatcher)
            register(WatchedDirectoryConvertedEvent.class, directoryWatcher)
            register(DirectoryUnsharedEvent.class, directoryWatcher)
            register(WatchedDirectoryConfigurationEvent.class, directoryWatcher)
        }
        
        log.info("initializing messenger")
        messenger = new Messenger(eventBus, home, i2pConnector, props)
        eventBus.with { 
            register(UILoadedEvent.class, messenger)
            register(MessageReceivedEvent.class, messenger)
            register(UIMessageEvent.class, messenger)
            register(UIMessageDeleteEvent.class, messenger)
            register(UIMessageReadEvent.class, messenger)
        }
    }

    public void startServices() {
        i2pSession.connect()
        hasherService.start()
        trustService.start()
        trustService.waitForLoad()
        hostCache.start({connectionManager.getConnections().collect{ it.endpoint.destination }} as Supplier)
        connectionManager.start()
        cacheClient.start()
        connectionAcceptor.start()
        connectionEstablisher.start()
        hostCache.waitForLoad()
        updateClient?.start()
        feedManager.start()
        feedClient.start()
        trackerResponder.start()
    }

    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            log.info("already shutting down")
            return
        }
        log.info("saving settings")
        saveMuSettings()
        log.info("shutting down host cache")
        hostCache.stop()
        log.info("shutting down trust subscriber")
        trustSubscriber.stop()
        log.info("shutting down trust service")
        trustService.stop()
        log.info("shutting down persister service")
        persisterService.stop()
        log.info("shutting down persisterFolder service")
        persisterFolderService.stop()
        log.info("shutting down collection manager")
        collectionManager.stop()
        log.info("shutting down download manager")
        downloadManager.shutdown()
        log.info("shutting down connection acceptor")
        connectionAcceptor.stop()
        log.info("shutting down connection establisher")
        connectionEstablisher.stop()
        log.info("shutting down directory watcher")
        directoryWatcher.stop()
        log.info("shutting down watch directory manager")
        watchedDirectoryManager.shutdown()
        log.info("shutting down cache client")
        cacheClient.stop()
        log.info("shutting down chat server")
        chatServer.stop()
        log.info("shutting down chat manager")
        chatManager.shutdown()
        log.info("shutting down feed manager")
        feedManager.stop()
        log.info("shutting down feed client")
        feedClient.stop()
        log.info("shutting down tracker responder")
        trackerResponder.stop()
        log.info("shutting down connection manager")
        connectionManager.shutdown()
        if (updateClient != null) {
            log.info("shutting down update client")
            updateClient.stop()
        }
        log.info("shutting down messenger")
        messenger.stop()
        log.info("killing socket manager")
        i2pSocketManager.destroySocketManager()
        if (router != null) {
            log.info("shutting down embedded router")
            router.shutdown(0)
        }
        log.info("shutting down event bus");
        eventBus.shutdown()
        log.info("shutdown complete")
    }
    
    public void saveMuSettings() {
        File f = new File(home, "MuWire.properties")
        f.withPrintWriter("UTF-8", { muOptions.write(it) })
    }
    
    public void saveI2PSettings() {
        File f = new File(home, "i2p.properties")
        f.withOutputStream { i2pOptions.store(it, "I2P Options") }
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

        Core core = new Core(props, home, "0.8.3")
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
