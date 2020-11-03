
package com.muwire.gui

import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Calendar
import java.util.UUID

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JOptionPane
import javax.swing.JTable
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.RouterDisconnectedEvent
import com.muwire.core.SharedFile
import com.muwire.core.collections.CollectionDownloadedEvent
import com.muwire.core.collections.CollectionLoadedEvent
import com.muwire.core.collections.CollectionUnsharedEvent
import com.muwire.core.collections.FileCollection
import com.muwire.core.collections.UICollectionCreatedEvent
import com.muwire.core.connection.ConnectionAttemptStatus
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.connection.DisconnectionEvent
import com.muwire.core.content.ContentControlEvent
import com.muwire.core.download.DownloadStartedEvent
import com.muwire.core.download.Downloader
import com.muwire.core.filecert.CertificateCreatedEvent
import com.muwire.core.filefeeds.Feed
import com.muwire.core.filefeeds.FeedFetchEvent
import com.muwire.core.filefeeds.FeedItemFetchedEvent
import com.muwire.core.filefeeds.FeedLoadedEvent
import com.muwire.core.filefeeds.UIDownloadFeedItemEvent
import com.muwire.core.filefeeds.UIFeedConfigurationEvent
import com.muwire.core.files.AllFilesLoadedEvent
import com.muwire.core.files.DirectoryUnsharedEvent
import com.muwire.core.files.DirectoryWatchedEvent
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileHashingEvent
import com.muwire.core.files.FileLoadedEvent
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.files.SideCarFileEvent
import com.muwire.core.messenger.MWMessage
import com.muwire.core.messenger.MessageLoadedEvent
import com.muwire.core.messenger.MessageReceivedEvent
import com.muwire.core.messenger.MessageSentEvent
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.search.UIResultBatchEvent
import com.muwire.core.search.UIResultEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustService
import com.muwire.core.trust.TrustSubscriptionEvent
import com.muwire.core.trust.TrustSubscriptionUpdatedEvent
import com.muwire.core.update.UpdateAvailableEvent
import com.muwire.core.update.UpdateDownloadedEvent
import com.muwire.core.upload.UploadEvent
import com.muwire.core.upload.UploadFinishedEvent
import com.muwire.core.upload.Uploader
import com.muwire.core.util.BandwidthCounter

import static com.muwire.gui.Translator.trans

import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonModel
import griffon.core.env.Metadata
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.transform.FXObservable
import griffon.transform.Observable
import net.i2p.data.Base64
import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class MainFrameModel {
    @Inject Metadata metadata
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    MainFrameController controller
    @MVCMember @Nonnull
    MainFrameView view
    @Inject @Nonnull GriffonApplication application
    @Observable boolean coreInitialized = false
    @Observable boolean routerPresent

    def results = new ConcurrentHashMap<>()
    def browses = new ConcurrentHashSet<String>()
    def collections = new ConcurrentHashSet<String>()
    List<FileCollection> localCollections = Collections.synchronizedList(new ArrayList<>())
    List<SharedFile> collectionFiles = new ArrayList<>()
    def downloads = []
    def uploads = []
    boolean treeVisible = true
    def shared 
    TreeModel sharedTree 
    def treeRoot
    final Map<SharedFile, TreeNode> fileToNode = new HashMap<>()
    def connectionList = []
    def searches = new LinkedList()
    def trusted = []
    def distrusted = []
    def subscriptions = []
    def feeds = []
    def feedItems = []
    
    def messageFolders = [trans("INBOX"), trans("OUTBOX"), trans("SENT")] 
    List<MWMessage> messageHeaders = new ArrayList<>()
    Map<Integer, Set<MWMessage>> messageHeadersMap = new HashMap<>()
    int folderIdx
    
    private final static int INBOX = 0
    private final static int OUTBOX = 1
    private final static int SENT = 2
    
    boolean sessionRestored

    @Observable int connections
    @Observable String me
    @Observable int loadedFiles
    @Observable File hashingFile
    @Observable boolean cancelButtonEnabled
    @Observable boolean retryButtonEnabled
    @Observable boolean pauseButtonEnabled
    @Observable boolean clearButtonEnabled
    @Observable boolean previewButtonEnabled
    @Observable String resumeButtonText
    @Observable boolean addCommentButtonEnabled
    @Observable boolean publishButtonEnabled
    @Observable String publishButtonText
    @Observable boolean updateFileFeedButtonEnabled
    @Observable boolean unsubscribeFileFeedButtonEnabled
    @Observable boolean configureFileFeedButtonEnabled
    @Observable boolean downloadFeedItemButtonEnabled
    @Observable boolean viewFeedItemCommentButtonEnabled
    @Observable boolean viewFeedItemCertificatesButtonEnabled
    @Observable boolean subscribeButtonEnabled
    @Observable boolean markNeutralFromTrustedButtonEnabled
    @Observable boolean markDistrustedButtonEnabled
    @Observable boolean browseFromTrustedButtonEnabled
    @Observable boolean chatFromTrustedButtonEnabled
    @Observable boolean markNeutralFromDistrustedButtonEnabled
    @Observable boolean markTrustedButtonEnabled
    @Observable boolean reviewButtonEnabled
    @Observable boolean updateButtonEnabled
    @Observable boolean unsubscribeButtonEnabled
    @Observable boolean collectionButtonEnabled
    
    @Observable boolean viewCollectionCommentButtonEnabled
    @Observable boolean viewItemCommentButtonEnabled
    @Observable boolean deleteCollectionButtonEnabled 
    
    @Observable boolean messageButtonsEnabled
    
    @Observable boolean searchesPaneButtonEnabled
    @Observable boolean downloadsPaneButtonEnabled
    @Observable boolean uploadsPaneButtonEnabled
    @Observable boolean collectionsPaneButtonEnabled
    @Observable boolean monitorPaneButtonEnabled
    @Observable boolean feedsPaneButtonEnabled
    @Observable boolean trustPaneButtonEnabled
    @Observable boolean messagesPaneButtonEnabled
    @Observable boolean chatPaneButtonEnabled
    
    @Observable boolean chatServerRunning
    
    @Observable Downloader downloader
    
    @Observable int downSpeed
    @Observable int upSpeed

    private final Set<InfoHash> downloadInfoHashes = new ConcurrentHashSet<>()

    @Observable volatile Core core

    private long lastRetryTime = System.currentTimeMillis()

    UISettings uiSettings

    void updateTablePreservingSelection(String tableName) {
        def downloadTable = builder.getVariable(tableName)
        int selectedRow = downloadTable.getSelectedRow()
        while(true) {
            try {
                downloadTable.model.fireTableDataChanged()
                break
            } catch (IllegalArgumentException iae) {} // caused by underlying model changing while table is sorted
        }
        downloadTable.selectionModel.setSelectionInterval(selectedRow,selectedRow)
    }

    void mvcGroupInit(Map<String, Object> args) {

        uiSettings = application.context.get("ui-settings")
        
        shared = []
        treeRoot = new DefaultMutableTreeNode()
        sharedTree = new DefaultTreeModel(treeRoot)
        
        messageHeadersMap.put(0, new LinkedHashSet<>())
        messageHeadersMap.put(1, new LinkedHashSet<>())
        messageHeadersMap.put(2, new LinkedHashSet<>())

        Timer timer = new Timer("download-pumper", true)
        timer.schedule({
            runInsideUIAsync {
                if (!mvcGroup.alive)
                    return

                // remove cancelled or finished or hopeless downloads
                if (!clearButtonEnabled || uiSettings.clearCancelledDownloads || uiSettings.clearFinishedDownloads) {
                    def toRemove = []
                    downloads.each {
                        def state = it.downloader.getCurrentState()
                        if (state == Downloader.DownloadState.CANCELLED) {
                            if (uiSettings.clearCancelledDownloads) {
                                toRemove << it
                            } else {
                                clearButtonEnabled = true
                            }
                        } else if (state == Downloader.DownloadState.FINISHED) {
                            if (uiSettings.clearFinishedDownloads) {
                                toRemove << it
                            } else {
                                clearButtonEnabled = true
                            }
                        } else if (state == Downloader.DownloadState.HOPELESS) {
                            clearButtonEnabled = true
                        }
                    }
                    toRemove.each {
                        downloads.remove(it)
                    }
                }

                updateTablePreservingSelection("uploads-table")
                updateTablePreservingSelection("downloads-table")
                updateTablePreservingSelection("trusted-table")
                updateTablePreservingSelection("distrusted-table")
                
                int totalUpload = 0
                uploads.each { 
                    totalUpload += it.speed()
                }
                upSpeed = totalUpload
                if (core != null) 
                    downSpeed = core.downloadManager.totalDownloadSpeed()
            }
        }, 1000, 1000)

        application.addPropertyChangeListener("core", {e ->
            coreInitialized = (e.getNewValue() != null)
            core = e.getNewValue()
            routerPresent = core.router != null
            me = core.me.getHumanReadableName()
            core.eventBus.register(UIResultBatchEvent.class, this)
            core.eventBus.register(DownloadStartedEvent.class, this)
            core.eventBus.register(ConnectionEvent.class, this)
            core.eventBus.register(DisconnectionEvent.class, this)
            core.eventBus.register(FileHashedEvent.class, this)
            core.eventBus.register(SideCarFileEvent.class, this)
            core.eventBus.register(FileHashingEvent.class, this)
            core.eventBus.register(FileLoadedEvent.class, this)
            core.eventBus.register(UploadEvent.class, this)
            core.eventBus.register(UploadFinishedEvent.class, this)
            core.eventBus.register(TrustEvent.class, this)
            core.eventBus.register(QueryEvent.class, this)
            core.eventBus.register(UpdateAvailableEvent.class, this)
            core.eventBus.register(FileDownloadedEvent.class, this)
            core.eventBus.register(FileUnsharedEvent.class, this)
            core.eventBus.register(RouterDisconnectedEvent.class, this)
            core.eventBus.register(AllFilesLoadedEvent.class, this)
            core.eventBus.register(UpdateDownloadedEvent.class, this)
            core.eventBus.register(TrustSubscriptionUpdatedEvent.class, this)
            core.eventBus.register(SearchEvent.class, this)
            core.eventBus.register(CertificateCreatedEvent.class, this)
            core.eventBus.register(FeedLoadedEvent.class, this)
            core.eventBus.register(FeedFetchEvent.class, this)
            core.eventBus.register(FeedItemFetchedEvent.class, this)
            core.eventBus.register(UIFeedConfigurationEvent.class, this)
            core.eventBus.register(CollectionLoadedEvent.class, this)
            core.eventBus.register(UICollectionCreatedEvent.class, this)
            core.eventBus.register(CollectionDownloadedEvent.class, this)
            core.eventBus.register(CollectionUnsharedEvent.class, this)
            core.eventBus.register(MessageLoadedEvent.class, this)
            core.eventBus.register(MessageReceivedEvent.class, this)
            core.eventBus.register(MessageSentEvent.class, this)

            core.muOptions.watchedKeywords.each {
                core.eventBus.publish(new ContentControlEvent(term : it, regex: false, add: true))
            }
            core.muOptions.watchedRegexes.each {
                core.eventBus.publish(new ContentControlEvent(term : it, regex: true, add: true))
            }
            
            chatServerRunning = core.chatServer.isRunning()
            
            timer.schedule({
                if (core.shutdown.get())
                    return
                int retryInterval = core.muOptions.downloadRetryInterval
                if (retryInterval > 0) {
                    retryInterval *= 1000
                    long now = System.currentTimeMillis()
                    if (now - lastRetryTime > retryInterval) {
                        lastRetryTime = now
                        runInsideUIAsync {
                            downloads.each {
                                def state = it.downloader.currentState
                                if (state == Downloader.DownloadState.FAILED ||
                                    state == Downloader.DownloadState.DOWNLOADING)
                                    it.downloader.resume()
                                updateTablePreservingSelection("downloads-table")
                            }
                        }

                    }
                }
            }, 1000, 1000)

            runInsideUIAsync {
                trusted.addAll(core.trustService.good.values())
                distrusted.addAll(core.trustService.bad.values())

                resumeButtonText = "RETRY"
                publishButtonText = "PUBLISH"
                
                searchesPaneButtonEnabled = false
                downloadsPaneButtonEnabled = true
                uploadsPaneButtonEnabled = true
                monitorPaneButtonEnabled = true
                feedsPaneButtonEnabled = true
                trustPaneButtonEnabled = true
                chatPaneButtonEnabled = true
                
                if (core.muOptions.startChatServer)
                    controller.startChatServer()
            }
        })

    }

    void onAllFilesLoadedEvent(AllFilesLoadedEvent e) {
        runInsideUIAsync {
            view.magicTreeExpansion()
            core.muOptions.trustSubscriptions.each {
                core.eventBus.publish(new TrustSubscriptionEvent(persona : it, subscribe : true))
            }
        }
    }

    void onUpdateDownloadedEvent(UpdateDownloadedEvent e) {
        runInsideUIAsync {
            Map<String, Object> args = new HashMap<>()
            args['available'] = null
            args['downloaded'] = e
            mvcGroup.createMVCGroup("update", "update", args)
        }
    }

    void onUIResultEvent(UIResultEvent e) {
        MVCGroup resultsGroup = results.get(e.uuid)
        resultsGroup?.model.handleResult(e)
    }

    void onUIResultBatchEvent(UIResultBatchEvent e) {
        MVCGroup resultsGroup = results.get(e.uuid)
        resultsGroup?.model?.handleResultBatch(e.results)
    }

    void onDownloadStartedEvent(DownloadStartedEvent e) {
        runInsideUIAsync {
            downloads << e
            downloadInfoHashes.add(e.downloader.infoHash)
        }
    }

    void onConnectionEvent(ConnectionEvent e) {
        if (e.getStatus() != ConnectionAttemptStatus.SUCCESSFUL)
            return
        runInsideUIAsync {
            connections = core.connectionManager.getConnections().size()

            view.showRestoreOrEmpty()
            
            if (connections > 0) {
                def topPanel = builder.getVariable("top-panel")
                topPanel.getLayout().show(topPanel, "top-search-panel")
            }

            UIConnection con = new UIConnection(destination : e.endpoint.destination, incoming : e.incoming)
            connectionList.add(con)
            JTable table = builder.getVariable("connections-table")
            table.model.fireTableDataChanged()
        }
    }

    void onDisconnectionEvent(DisconnectionEvent e) {
        runInsideUIAsync {
            connections = core.connectionManager.getConnections().size()

            if (connections == 0) {
                def topPanel = builder.getVariable("top-panel")
                topPanel.getLayout().show(topPanel, "top-connect-panel")
            }

            UIConnection con = new UIConnection(destination : e.destination)
            connectionList.remove(con)
            JTable table = builder.getVariable("connections-table")
            table.model.fireTableDataChanged()
        }
    }

    void onFileHashingEvent(FileHashingEvent e) {
        runInsideUIAsync {
            hashingFile = e.hashingFile
        }
    }

    void onFileHashedEvent(FileHashedEvent e) {
        runInsideUIAsync {
            hashingFile = null
        }
        if (e.error != null)
            return // TODO do something
        runInsideUIAsync {
            shared << e.sharedFile
            loadedFiles = shared.size()
            JTable table = builder.getVariable("shared-files-table")
            table.model.fireTableDataChanged()
            insertIntoTree(e.sharedFile)
            loadedFiles = fileToNode.size()
        }
    }

    void onFileLoadedEvent(FileLoadedEvent e) {
        if (e.source == "PersisterService")
            return
        runInsideUIAsync {
            shared << e.loadedFile
            loadedFiles = shared.size()
            JTable table = builder.getVariable("shared-files-table")
            table.model.fireTableDataChanged()
            insertIntoTree(e.loadedFile)
            loadedFiles = fileToNode.size()
        }
    }

    void onFileUnsharedEvent(FileUnsharedEvent e) {
        runInsideUIAsync {
            shared.remove(e.unsharedFile)
            loadedFiles = shared.size()
            
            def dmtn = fileToNode.remove(e.unsharedFile)
            if (dmtn != null) {
                loadedFiles = fileToNode.size()
                List<File> unshared = new ArrayList<>()
                while (true) {
                    def parent = dmtn.getParent()
                    parent.remove(dmtn)
                    if (parent == treeRoot)
                        break
                    if (parent.getChildCount() == 0) {
                        File file = parent.getUserObject().file
                        if (core.watchedDirectoryManager.isWatched(file))
                            unshared.add(file)
                        dmtn = parent
                        continue
                    }
                    break
                }
                if (!unshared.isEmpty()) {
                    File unsharedRoot = unshared.get( unshared.size() -1 )
                    core.eventBus.publish(new DirectoryUnsharedEvent(directory : unsharedRoot))
                }
            } 
            view.refreshSharedFiles()
        }
    }

    void onUploadEvent(UploadEvent e) {
        runInsideUIAsync {
            UploaderWrapper wrapper = null
            uploads.each {
                if (it.uploader == e.uploader) {
                    wrapper = it
                    return
                }
            }
            if (wrapper != null) 
                wrapper.updateUploader(e.uploader)
            else
                uploads << new UploaderWrapper(uploader : e.uploader)
            updateTablePreservingSelection("uploads-table")
            view.refreshSharedFiles()
        }
    }

    void onUploadFinishedEvent(UploadFinishedEvent e) {
        runInsideUIAsync {
            UploaderWrapper wrapper = null
            uploads.each {
                if (it.uploader == e.uploader) {
                    wrapper = it
                    return
                }
            }
            if (uiSettings.clearUploads) {
                uploads.remove(wrapper)
            } else {
                wrapper.finished = true
            }
            updateTablePreservingSelection("uploads-table")
        }
    }

    void onTrustEvent(TrustEvent e) {
        runInsideUIAsync {

            trusted.clear()
            trusted.addAll(core.trustService.good.values())
            distrusted.clear()
            distrusted.addAll(core.trustService.bad.values())

            updateTablePreservingSelection("trusted-table")
            updateTablePreservingSelection("distrusted-table")

            results.values().each { MVCGroup group ->
                if (group.alive) {
                    group.view.pane.getClientProperty("results-table")?.model.fireTableDataChanged()
                }
            }
        }
    }

    void onTrustSubscriptionUpdatedEvent(TrustSubscriptionUpdatedEvent e) {
        runInsideUIAsync {
            if (!subscriptions.contains(e.trustList))
                subscriptions << e.trustList
            updateTablePreservingSelection("subscription-table")
        }
    }
    
    void onSearchEvent(SearchEvent e) {
        runInsideUIAsync {
            view.refreshSharedFiles()
        }
    }

    void onQueryEvent(QueryEvent e) {
        if (!uiSettings.showMonitor)
            return
            
        if (e.replyTo == core.me.destination)
            return

        def search
        if (e.searchEvent.searchHash != null) {
            if (!uiSettings.showSearchHashes) {
                return
            }
            search = Base64.encode(e.searchEvent.searchHash)
        } else {
            StringBuilder sb = new StringBuilder()
            e.searchEvent.searchTerms?.each {
                sb.append(it)
                sb.append(" ")
            }
            search = sb.toString()
            if (search.trim().size() == 0)
                return
        }
        runInsideUIAsync {
            JTable table = builder.getVariable("searches-table")

            Boolean searchFound = false
            Iterator searchIter = searches.iterator()
            while ( searchIter.hasNext() ) {
                IncomingSearch searchEle = searchIter.next()
                if ( searchEle.search == search
                        && searchEle.originator == e.originator
                        && searchEle.uuid == e.searchEvent.getUuid() ) {
                    searchIter.remove()
                    table.model.fireTableDataChanged()
                    searchFound = true
                    searchEle.count++
                    searchEle.timestamp = Calendar.getInstance()
                    searches.addFirst(searchEle)
                    break
                }
            }

            if (!searchFound) {
                searches.addFirst(new IncomingSearch(search, e.replyTo, e.originator, e.searchEvent.getUuid()))
            }

            while(searches.size() > 200)
                searches.removeLast()

            table.model.fireTableDataChanged()
        }
    }

    class IncomingSearch {
        String search
        Destination replyTo
        Persona originator
        long count
        UUID uuid
        Calendar timestamp

        IncomingSearch( String search, Destination replyTo, Persona originator, UUID uuid ) {
            this.search = search
            this.replyTo = replyTo
            this.originator = originator
            this.uuid = uuid
            this.count = 1
            this.timestamp = Calendar.getInstance()
        }
    }

    void onUpdateAvailableEvent(UpdateAvailableEvent e) {
        runInsideUIAsync {
            Map<String, Object> args = new HashMap<>()
            args['available'] = e
            args['downloaded'] = null
            mvcGroup.createMVCGroup("update", "update", args)
        }
    }

    void onRouterDisconnectedEvent(RouterDisconnectedEvent e) {
        if (core.getShutdown().get())
            return
        runInsideUIAsync {
            JOptionPane.showMessageDialog(null, "MuWire lost connection to the I2P router and will now exit.",
                "Connection to I2P router lost", JOptionPane.WARNING_MESSAGE)
            System.exit(0)
        }
    }

    void onFileDownloadedEvent(FileDownloadedEvent e) {
        if (!core.muOptions.shareDownloadedFiles)
            return
        runInsideUIAsync {
            shared << e.downloadedFile
            JTable table = builder.getVariable("shared-files-table")
            table.model.fireTableDataChanged()
            insertIntoTree(e.downloadedFile)
            loadedFiles = fileToNode.size()
        }
    }
    
    void onSideCarFileEvent(SideCarFileEvent e) {
        runInsideUIAsync {
            view.refreshSharedFiles()
        }
    }
    
    void onCertificateCreatedEvent(CertificateCreatedEvent e) {
        runInsideUIAsync {
            view.refreshSharedFiles()
        }
    }
    
    private void insertIntoTree(SharedFile file) {
        List<File> parents = new ArrayList<>()
        File tmp = file.file.getParentFile()
        while(tmp.getParent() != null) {
            parents << tmp
            tmp = tmp.getParentFile()
        }
        Collections.reverse(parents)
        TreeNode node = treeRoot
        for(File path : parents) {
            boolean exists = false
            def children = node.children()
            def child = null
            while(children.hasMoreElements()) {
                child = children.nextElement()
                def userObject = child.getUserObject()
                if (userObject != null && userObject.file == path) {
                    exists = true
                    break
                }
            }
            if (!exists) {
                child = new DefaultMutableTreeNode(new InterimTreeNode(path))
                node.add(child)
            }
            node = child
        }
        
        def dmtn = new DefaultMutableTreeNode(file)
        fileToNode.put(file, dmtn)
        node.add(dmtn)
        view.refreshSharedFiles()
    }

    private static class UIConnection {
        Destination destination
        boolean incoming

        @Override
        public int hashCode() {
            destination.hashCode()
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof UIConnection))
                return false
            UIConnection other = (UIConnection) o
            return destination == other.destination
        }
    }

    boolean canDownload(InfoHash hash) {
        !downloadInfoHashes.contains(hash)
    }
    
    class UploaderWrapper {
        Uploader uploader
        int requests
        boolean finished
        
        private BandwidthCounter bwCounter = new BandwidthCounter(0)
        
        public int speed() {
            
            if (finished) 
                return 0

            initIfNeeded()
            bwCounter.read(uploader.dataSinceLastRead())
            bwCounter.average()
        }
        
        void updateUploader(Uploader uploader) {
            initIfNeeded()
            bwCounter.read(this.uploader.dataSinceLastRead())
            this.uploader = uploader
            requests++
            finished = false
        }
        
        private void initIfNeeded() {
            if (bwCounter.getMemory() != core.muOptions.speedSmoothSeconds)
                bwCounter = new BandwidthCounter(core.muOptions.speedSmoothSeconds)
        }
    }
    
    void onFeedLoadedEvent(FeedLoadedEvent e) {
        runInsideUIAsync {
            feeds << e.feed
            view.refreshFeeds()
        }
    }
    
    void onFeedFetchEvent(FeedFetchEvent e) {
        runInsideUIAsync {
            view.refreshFeeds()
        }
    }
    
    void onUIFeedConfigurationEvent(UIFeedConfigurationEvent e) {
        if (!e.newFeed)
            return
        runInsideUIAsync {
            if (feeds.contains(e.feed))
                return
            feeds << e.feed
            view.refreshFeeds()
        }
    }
    
    void onFeedItemFetchedEvent(FeedItemFetchedEvent e) {
        Feed feed = core.feedManager.getFeed(e.item.getPublisher())
        if (feed == null || !feed.isAutoDownload())
            return
        if (!canDownload(e.item.getInfoHash()))
            return
        if (core.fileManager.isShared(e.item.getInfoHash()))
            return
            
        File target = new File(core.getMuOptions().getDownloadLocation(), e.item.getName())    
        core.eventBus.publish(new UIDownloadFeedItemEvent(item : e.item, target : target, sequential : feed.isSequential()))
    }
    
    void onCollectionLoadedEvent(CollectionLoadedEvent e) {
        if (!e.local)
            return 
        runInsideUIAsync {
            localCollections.add(e.collection)
            view.collectionsTable.model.fireTableDataChanged()
        }
    }
    
    void onUICollectionCreatedEvent(UICollectionCreatedEvent e) {
        runInsideUIAsync {
            localCollections.add(e.collection)
            view.collectionsTable.model.fireTableDataChanged()
        }
    }
    
    void onCollectionDownloadedEvent(CollectionDownloadedEvent e) {
        if (!core.muOptions.shareDownloadedFiles)
            return
        runInsideUIAsync {
            localCollections.add(e.collection)
            view.collectionsTable.model.fireTableDataChanged()
        }
    }
    
    void onCollectionUnsharedEvent(CollectionUnsharedEvent e) {
        runInsideUIAsync {
            localCollections.remove(e.collection)
            view.collectionsTable.model.fireTableDataChanged()
            collectionFiles.clear()
            view.collectionFilesTable.model.fireTableDataChanged()
        }
    }
    
    void onMessageLoadedEvent(MessageLoadedEvent e) {
        runInsideUIAsync {
            int idx = 0
            switch(e.folder) {
                case "inbox" : idx = INBOX; break
                case "outbox" : idx = OUTBOX; break
                case "sent" : idx = SENT; break
                default :
                    throw new IllegalStateException("unknown folder $e.folder")

            }

            messageHeadersMap.get(idx).add(e.message)
            if (idx == folderIdx) {
                messageHeaders.clear()
                messageHeaders.addAll(messageHeadersMap.get(idx))
                view.messageHeaderTable.model.fireTableDataChanged()
            }
        }
    }
    
    void onMessageReceivedEvent(MessageReceivedEvent e) {
        runInsideUIAsync {
            messageHeadersMap.get(INBOX).add(e.message)
            if (folderIdx == INBOX) {
                messageHeaders.clear()
                messageHeaders.addAll(messageHeadersMap.get(INBOX))
                view.messageHeaderTable.model.fireTableDataChanged()
            }
        }
    }
    
    void onMessageSentEvent(MessageSentEvent e) {
        runInsideUIAsync {
            messageHeadersMap.get(OUTBOX).remove(e.message)
            messageHeadersMap.get(SENT).add(e.message)
            if (folderIdx != INBOX) {
                messageHeaders.clear()
                messageHeaders.addAll(messageHeadersMap.get(idx))
                view.messageHeaderTable.model.fireTableDataChanged()
            }
        }
    }
}