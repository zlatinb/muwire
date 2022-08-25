
package com.muwire.gui

import com.muwire.core.download.DownloadHopelessEvent
import com.muwire.core.files.DirectoryUnsharedEvent
import com.muwire.core.files.FileModifiedEvent
import com.muwire.core.messenger.MessageFolderLoadingEvent
import com.muwire.core.profile.MWProfileHeader
import com.muwire.core.search.ResultsEvent
import com.muwire.core.trust.RemoteTrustList
import com.muwire.core.trust.TrustServiceLoadedEvent
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.ThumbnailIcon
import com.muwire.gui.profile.TrustPOP
import griffon.core.controller.ControllerAction

import javax.swing.DefaultListModel
import javax.swing.Icon
import javax.swing.SwingWorker
import java.awt.Image
import java.awt.TrayIcon
import java.awt.Window
import java.util.concurrent.ConcurrentHashMap

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JOptionPane
import javax.swing.JTable
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode

import static com.muwire.gui.Translator.trans

import com.muwire.core.Core
import com.muwire.core.InfoHash
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
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileHashingEvent
import com.muwire.core.files.FileLoadedEvent
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.files.SideCarFileEvent
import com.muwire.core.messenger.MWMessage
import com.muwire.core.messenger.MessageLoadedEvent
import com.muwire.core.messenger.MessageReceivedEvent
import com.muwire.core.messenger.MessageSentEvent
import com.muwire.core.messenger.Messenger
import com.muwire.core.messenger.UIMessageReadEvent
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.search.UIResultBatchEvent
import com.muwire.core.search.UIResultEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustSubscriptionEvent
import com.muwire.core.trust.TrustSubscriptionUpdatedEvent
import com.muwire.core.update.UpdateAvailableEvent
import com.muwire.core.update.UpdateDownloadedEvent
import com.muwire.core.upload.UploadEvent
import com.muwire.core.upload.UploadFinishedEvent
import com.muwire.core.upload.Uploader
import com.muwire.core.util.BandwidthCounter

import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonModel
import griffon.core.env.Metadata
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.transform.Observable
import net.i2p.data.Base64
import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet
import griffon.metadata.ArtifactProviderFor

import java.util.stream.Collectors

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
    def resultDetails = new ConcurrentHashSet<String>()
    
    List<FileCollection> localCollections = Collections.synchronizedList(new ArrayList<>())
    List<SharedFile> collectionFiles = new ArrayList<>()
    def downloads = []
    
    // uploads table
    def uploads = []
    
    // Library model
    @Observable boolean filteringEnabled
    @Observable boolean clearFilterActionEnabled
    volatile String[] filter
    volatile Filterer filterer
    boolean treeVisible = true
    private final Set<SharedFile> allSharedFiles = Collections.synchronizedSet(new LinkedHashSet<>())
    List<SharedFile> shared
    final Map<SharedFile, Integer> sharedFileIdx = new HashMap<>()
    private boolean libraryDirty
    boolean libraryTabVisible
    private final javax.swing.Timer libraryTimer = new javax.swing.Timer(1000, {refreshLibrary()})
    LibraryTreeModel allFilesSharedTree, sharedTree 
    SortedTreeNode<SharedFile> allFilesTreeRoot, treeRoot
    final Map<SharedFile, TreeNode> fileToNode = new HashMap<>()
    
    def connectionList = []
    def searches = new LinkedList()
    List<TrustPOP> trustedContacts = []
    List<TrustPOP> distrustedContacts = []
    List<SubscriptionPOP> subscriptions = []
    def feeds = []
    def feedItems = []
    
    final DefaultListModel messageFolderListModel = new DefaultListModel()
    final DefaultListModel userMessageFolderListModel = new DefaultListModel()
    final Map<String, MVCGroup> messageFoldersMap = new HashMap<>()
    final List<MVCGroup> messageFolders = [] 
    
    String folderIdx
    MessageNotificator messageNotificator
    
    boolean sessionRestored

    @Observable int connections
    @Observable int messages
    @Observable String me
    @Observable int loadedFiles
    @Observable int hashingFiles
    @Observable File hashingFile
    @Observable boolean cancelButtonEnabled
    @Observable boolean retryButtonEnabled
    @Observable boolean pauseButtonEnabled
    @Observable boolean clearButtonEnabled
    @Observable boolean previewButtonEnabled
    @Observable String resumeButtonText
    @Observable boolean addCommentButtonEnabled
    @Observable boolean publishButtonEnabled
    @Observable boolean updateFileFeedButtonEnabled
    @Observable boolean unsubscribeFileFeedButtonEnabled
    @Observable boolean configureFileFeedButtonEnabled
    @Observable boolean downloadFeedItemButtonEnabled
    @Observable boolean viewFeedItemCommentButtonEnabled
    @Observable boolean viewFeedItemCertificatesButtonEnabled
    @Observable boolean subscribeButtonEnabled
    
    @Observable boolean markDistrustedButtonEnabled
    @Observable boolean browseFromTrustedButtonEnabled
    @Observable boolean chatFromTrustedButtonEnabled
    @Observable boolean messageFromTrustedButtonEnabled
    @Observable boolean markTrustedButtonEnabled
    
    @Observable boolean reviewButtonEnabled
    @Observable boolean updateButtonEnabled
    @Observable boolean unsubscribeButtonEnabled
    
    @Observable boolean viewCollectionCommentButtonEnabled
    @Observable boolean viewItemCommentButtonEnabled
    @Observable boolean deleteCollectionButtonEnabled
    
    @Observable boolean deleteMessageFolderButtonEnabled
    
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
    
    @Observable UpdateDownloadedEvent updateDownloadedEvent
    @Observable UpdateAvailableEvent updateAvailableEvent

    @Observable volatile Core core

    private long lastRetryTime = System.currentTimeMillis()

    UISettings uiSettings

    void updateTablePreservingSelection(String tableName) {
        JTable table = builder.getVariable(tableName)
        int[] selectedRows = table.getSelectedRows()
        for (int i = 0; i < selectedRows.length; i ++)
            selectedRows[i] = table.rowSorter.convertRowIndexToModel(selectedRows[i])
        while(true) {
            try {
                table.model.fireTableDataChanged()
                break
            } catch (IllegalArgumentException iae) {} // caused by underlying model changing while table is sorted
        }
        for (int i = 0; i < selectedRows.length; i ++) {
            if (selectedRows[i] >= table.model.getRowCount())
                selectedRows[i] = -1
            else
                selectedRows[i] = table.rowSorter.convertRowIndexToView(selectedRows[i])
        }
        for (int selectedRow : selectedRows) {
            if (selectedRow >= 0)
                table.selectionModel.addSelectionInterval(selectedRow,selectedRow)
        }
    }

    void mvcGroupInit(Map<String, Object> args) {

        uiSettings = application.context.get("ui-settings")
        
        messageNotificator = new MessageNotificator(uiSettings,
                (NotifyService) application.context.get("notify-service"),
                (Window) application.getWindowManager().findWindow("main-frame"),
                (Image) view.builder.imageIcon("/email.png").image)
        
        shared = []
        treeRoot = new LibraryTreeModel.LibraryTreeNode()
        sharedTree = new LibraryTreeModel(treeRoot)
        allFilesTreeRoot = new LibraryTreeModel.LibraryTreeNode()
        allFilesSharedTree = new LibraryTreeModel(allFilesTreeRoot)

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
                updateTablePreservingSelection("trusted-contacts-table")
                updateTablePreservingSelection("distrusted-contacts-table")
                
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
            core.eventBus.register(TrustServiceLoadedEvent.class, this)
            core.eventBus.register(QueryEvent.class, this)
            core.eventBus.register(UpdateAvailableEvent.class, this)
            core.eventBus.register(FileDownloadedEvent.class, this)
            core.eventBus.register(FileUnsharedEvent.class, this)
            core.eventBus.register(FileModifiedEvent.class, this)
            core.eventBus.register(DirectoryUnsharedEvent.class, this)
            core.eventBus.register(RouterDisconnectedEvent.class, this)
            core.eventBus.register(AllFilesLoadedEvent.class, this)
            core.eventBus.register(UpdateDownloadedEvent.class, this)
            core.eventBus.register(TrustSubscriptionUpdatedEvent.class, this)
            core.eventBus.register(ResultsEvent.class, this)
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
            core.eventBus.register(UIMessageReadEvent.class, this)
            core.eventBus.register(MessageSentEvent.class, this)
            core.eventBus.register(MessageFolderLoadingEvent.class, this)

            libraryTimer.start()
            
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
                                    state == Downloader.DownloadState.DOWNLOADING ||
                                    state == Downloader.DownloadState.REJECTED)
                                    it.downloader.resume()
                            }
                            updateTablePreservingSelection("downloads-table")
                        }

                    }
                }
            }, 1000, 1000)

            runInsideUIAsync {
                trustedContacts.addAll(core.trustService.good.values().collect {new TrustPOP(it)})
                distrustedContacts.addAll(core.trustService.bad.values().collect {new TrustPOP(it)})

                resumeButtonText = "RETRY"
                
                searchesPaneButtonEnabled = false
                downloadsPaneButtonEnabled = true
                uploadsPaneButtonEnabled = true
                monitorPaneButtonEnabled = true
                feedsPaneButtonEnabled = true
                trustPaneButtonEnabled = true
                chatPaneButtonEnabled = true

                initSystemMessageFolders()
                
                if (core.muOptions.startChatServer)
                    controller.startChatServer()
                
                // notify any listeners
                uiSettings.notifyListeners()
            }
        })

    }

    void onAllFilesLoadedEvent(AllFilesLoadedEvent e) {
        runInsideUIAsync {
            view.refreshSharedFiles()
            loadedFiles = allSharedFiles.size()
            libraryDirty = false
            view.magicTreeExpansion()
            filteringEnabled = true
            view.switchLibraryTitle()
            core.muOptions.trustSubscriptions.each {
                core.eventBus.publish(new TrustSubscriptionEvent(persona : it, subscribe : true))
            }
        }
    }

    void onUpdateDownloadedEvent(UpdateDownloadedEvent e) {
        runInsideUIAsync {
            updateDownloadedEvent = e
            updateAvailableEvent = null
        }
    }

    void onUpdateAvailableEvent(UpdateAvailableEvent e) {
        runInsideUIAsync {
            updateDownloadedEvent = null
            updateAvailableEvent = e
        }
    }
    
    void onUIResultEvent(UIResultEvent e) {
        MVCGroup resultsGroup = results.get(e.uuid)
        resultsGroup?.model.handleResult(e)
    }

    void onUIResultBatchEvent(UIResultBatchEvent e) {
        MVCGroup resultsGroup = results.get(e.uuid)
        if (resultsGroup == null)
            return
        if (resultsGroup.isAlive())
            resultsGroup.model.handleResultBatch(e.results)
    }

    void onDownloadStartedEvent(DownloadStartedEvent e) {
        runInsideUIAsync {
            downloads << e
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
            hashingFiles++
            hashingFile = e.hashingFile
        }
    }

    void onFileHashedEvent(FileHashedEvent e) {
        if (e.error != null)
            return // TODO do something
        runInsideUIAsync {
            hashingFiles--
            if (e.sharedFile.file == hashingFile)
                hashingFile = null
            if (e.duplicate != null) 
                allSharedFiles.remove(e.duplicate)
            allSharedFiles << e.sharedFile
            insertIntoTree(e.sharedFile, allFilesSharedTree, fileToNode)
            if (filter(e.sharedFile)) {
                insertIntoTable(e.sharedFile)
                insertIntoTree(e.sharedFile, sharedTree, null)
                libraryDirty = true
            }
        }
    }

    void onFileLoadedEvent(FileLoadedEvent e) {
        if (e.source == "PersisterService")
            return
        runInsideUIAsync {
            allSharedFiles << e.loadedFile
            insertIntoTree(e.loadedFile, allFilesSharedTree, fileToNode)
            insertIntoTable(e.loadedFile)
            insertIntoTree(e.loadedFile, sharedTree, null)
            libraryDirty = true
        }
    }
    
    private boolean filter(SharedFile sharedFile) {
        if (filter == null)
            return true
        String path = sharedFile.getFile().getAbsolutePath().toLowerCase()
        boolean contains = true
        for (String keyword : filter) {
            contains &= path.contains(keyword)
        }
        contains
    }
    
    void filterLibrary() {
        filterer?.cancel()
        view.clearSelectedFiles()
        shared.clear()
        sharedFileIdx.clear()
        treeRoot.removeAllChildren()
        view.refreshSharedFiles()
        if (filter != null) {
            setFilteringEnabled(false)
            setClearFilterActionEnabled(false)
            filterer = new Filterer()
            filterer.execute()
        } else {
            synchronized (allSharedFiles) {
                for (SharedFile sf : allSharedFiles)
                    insertIntoTable(sf)
            }
            shared.each {
                insertIntoTree(it, sharedTree, null)
            }
            view.refreshSharedFiles()
            view.magicTreeExpansion()
        }
    }
    
    private class Filterer extends SwingWorker<Void,SharedFile> {
        
        private volatile boolean cancelled
        
        void cancel() {
            cancelled = true
        }

        @Override
        protected Void doInBackground() throws Exception {
            allSharedFiles.stream().parallel().
                    filter({filter(it)}).
                    forEach({publish(it)})
            null
        }

        @Override
        protected void process(List<SharedFile> chunks) {
            if (cancelled || chunks.isEmpty())
                return
            for (SharedFile sf : chunks)
                insertIntoTable(sf)
            chunks.each {
                insertIntoTree(it, sharedTree, null)
            }
            view.refreshSharedFiles()
        }

        @Override
        protected void done() {
            if (cancelled)
                return
            setFilteringEnabled(true)
            setClearFilterActionEnabled(true)
            view.refreshSharedFiles()
            if (filter != null)
                view.fullTreeExpansion()
            else
                view.magicTreeExpansion()
        }
    }
    
    void onDirectoryUnsharedEvent(DirectoryUnsharedEvent event) {
        runInsideUIAsync {
            boolean modified = false
            for (File folder : event.directories) {
                modified |= sharedTree.removeFromTree(folder)
                modified |= allFilesSharedTree.removeFromTree(folder)
            }
            if (modified)
                view.refreshSharedFiles()
        }
    }
    
    void onFileModifiedEvent(FileModifiedEvent event) {
        // shortcut
        FileUnsharedEvent e = new FileUnsharedEvent(
                unsharedFiles: new SharedFile[]{event.sharedFile},
                deleted: false,
                implicit: false
        )
        onFileUnsharedEvent(e)
    }

    void onFileUnsharedEvent(FileUnsharedEvent e) {
        synchronized (allSharedFiles) {
            for (SharedFile sharedFile : e.unsharedFiles)
                allSharedFiles.remove(sharedFile)
        }
        runInsideUIAsync {
            synchronized (allSharedFiles) {
                shared.retainAll(allSharedFiles)
            }
            sharedFileIdx.clear()
            for (int i = 0; i < shared.size(); i++)
                sharedFileIdx.put(shared[i], i)
            loadedFiles = allSharedFiles.size()
            
            for (SharedFile sharedFile : e.unsharedFiles) {
                if (fileToNode.remove(sharedFile) == null)
                    continue
                if (e.implicit)
                    continue

                allFilesSharedTree.removeFromTree(sharedFile, e.deleted)
                sharedTree.removeFromTree(sharedFile, e.deleted)
            }
            
            view.refreshSharedFiles()
        }
    }

    void onUploadEvent(UploadEvent e) {
        runInsideUIAsync {
            int index = -1
            for(int i = 0; i < uploads.size(); i++) {
                if (uploads[i].uploader == e.uploader) {
                    index = i
                    break
                }
            }
            if (index >= 0) {
                uploads[index].updateUploader(e.uploader)
                view.refreshUploadsTableRow(index)
            }
            else {
                uploads << new UploaderWrapper(e.uploader, e.profileHeader)
                view.addUploadsTableRow(uploads.size() - 1)
                if (e.first) {
                    Set<SharedFile> sfs = core.fileManager.getSharedFiles(e.uploader.infoHash.getRoot())
                    sfs.stream().map({sharedFileIdx[it]}).
                            filter({it != null}).
                            forEach{view.refreshSharedFilesTableRow(it)}
                }
            }
        }
    }

    void onUploadFinishedEvent(UploadFinishedEvent e) {
        runInsideUIAsync {
            int index = -1
            for (int i = 0; i < uploads.size(); i++) {
                if (uploads[i].uploader == e.uploader) {
                    index = i
                    break
                }
            }
            if (index == -1)
                return // TODO: investigate how this is possible
            if (uiSettings.clearUploads) {
                uploads.remove(index)
                view.removeUploadsTableRow(index)
            } else {
                uploads[index].finished = true
            }
        }
    }

    void onTrustServiceLoadedEvent(TrustServiceLoadedEvent e) {
        runInsideUIAsync {
            refreshContacts()
        }
    }
    
    void onTrustEvent(TrustEvent e) {
        runInsideUIAsync {

            refreshContacts()
    
            results.values().each { MVCGroup group ->
                if (group.alive) {
                    group.view.onTrustChanged(e.persona)
                }
            }
        }
    }
    
    private void refreshContacts() {
        trustedContacts.clear()
        trustedContacts.addAll(core.trustService.good.values().collect{new TrustPOP(it)})
        distrustedContacts.clear()
        distrustedContacts.addAll(core.trustService.bad.values().collect{new TrustPOP(it)})

        updateTablePreservingSelection("trusted-contacts-table")
        updateTablePreservingSelection("distrusted-contacts-table")
    }

    void onTrustSubscriptionUpdatedEvent(TrustSubscriptionUpdatedEvent e) {
        runInsideUIAsync {
            def subPOP = new SubscriptionPOP(e.trustList)
            if (!subscriptions.contains(subPOP))
                subscriptions << subPOP
            updateTablePreservingSelection("subscription-table")
        }
    }
    
    void onResultsEvent(ResultsEvent e) {
        runInsideUIAsync {
            boolean affected = false
            for (SharedFile sf : e.results) {
                Integer row = sharedFileIdx[sf]
                if (row != null) {
                    affected = true
                    view.refreshSharedFilesTableRow(row)
                }
            }
            if (affected)
                view.fullUpdateIfColumnSorted("shared-files-table", 5)
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

    void onRouterDisconnectedEvent(RouterDisconnectedEvent e) {
        if (core.getShutdown().get())
            return
        if (core.router != null)
            return
        runInsideUIAsync {
            JOptionPane.showMessageDialog(null, trans("LOST_ROUTER_BODY"),
                trans("LOST_ROUTER_TITLE"), JOptionPane.WARNING_MESSAGE)
        }
    }

    void onFileDownloadedEvent(FileDownloadedEvent e) {
        if (!core.muOptions.shareDownloadedFiles || e.confidential)
            return
        runInsideUIAsync {
            allSharedFiles << e.downloadedFile
            insertIntoTree(e.downloadedFile, allFilesSharedTree, fileToNode)
            if (filter(e.downloadedFile)) {
                insertIntoTable(e.downloadedFile)
                insertIntoTree(e.downloadedFile,sharedTree, null)
                libraryDirty = true
            }
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
    
    private void insertIntoTree(SharedFile file, LibraryTreeModel libraryTreeModel, Map<SharedFile,TreeNode> f2n) {
        def leaf = libraryTreeModel.addToTree(file)
        f2n?.put(file, leaf)
    }
    
    private void insertIntoTable(SharedFile sharedFile) {
        int idx = shared.size()
        shared << sharedFile
        sharedFileIdx.put(sharedFile, idx)
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

    class UploaderWrapper implements PersonaOrProfile {
        Uploader uploader
        int requests
        boolean finished
        final MWProfileHeader profileHeader
        private Icon icon
        
        UploaderWrapper(Uploader uploader, MWProfileHeader profileHeader) {
            this.uploader = uploader
            this.profileHeader = profileHeader
        }
        
        private BandwidthCounter bwCounter = new BandwidthCounter(0)
        
        Persona getPersona() {
            uploader.downloaderPersona
        }
        
        Icon getThumbnail() {
            if (profileHeader == null)
                return null
            if (icon == null) {
                icon = new ThumbnailIcon(profileHeader.getThumbNail())
            }
            return icon
        }
        
        MWProfileHeader getHeader() {
            profileHeader
        }
        
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
    
    void addToOutbox(MWMessage message) {
        def status = new MWMessageStatus(message, false, null)
        messageFoldersMap.get(Messenger.OUTBOX).model.add(status)
    }
    
    void onMessageLoadedEvent(MessageLoadedEvent e) {
        runInsideUIAsync {
            messageFoldersMap.get(e.folder).model.processMessageLoadedEvent(e)
            if (e.unread) {
                messages++
                messageNotificator.messages(messages)
            }
        }
    }
    
    void onMessageReceivedEvent(MessageReceivedEvent e) {
        runInsideUIAsync {
            if (messageFoldersMap.get(Messenger.INBOX).model.processMessageReceivedEvent(e)) {
                messages++
                messageNotificator.newMessage(e.message.sender.getHumanReadableName())
                messageNotificator.messages(messages)
            }
        }
    }
    
    void onMessageSentEvent(MessageSentEvent e) {
        runInsideUIAsync {
            MWMessageStatus status = new MWMessageStatus(e.message, false, null)
            messageFoldersMap.get(Messenger.OUTBOX).model.remove(status)
            messageFoldersMap.get(Messenger.SENT).model.add(status)
        }
    }
    
    void onUIMessageReadEvent(UIMessageReadEvent e) {
        runInsideUIAsync {
            messages--
            messageNotificator.messages(messages)
        }
    }
    
    private void initSystemMessageFolders() {
        // create system mail folders
        def props = [:]
        props['core'] = core

        // inbox
        props['outgoing'] = false
        props['name'] = Messenger.INBOX
        props['txKey'] = "INBOX"
        def inbox = application.mvcGroupManager.createMVCGroup('message-folder', 'folder-inbox', props)
        view.addSystemMessageFolder(inbox)

        // outbox
        props['outgoing'] = true
        props['name'] = Messenger.OUTBOX
        props['txKey'] = "OUTBOX"
        def outbox = application.mvcGroupManager.createMVCGroup('message-folder', 'folder-outbox', props)
        view.addSystemMessageFolder(outbox)

        // sent
        props['outgoing'] = true
        props['name'] = Messenger.SENT
        props['txKey'] = "SENT"
        def sent = application.mvcGroupManager.createMVCGroup('message-folder', 'folder-sent', props)
        view.addSystemMessageFolder(sent)
    }
    
    void onMessageFolderLoadingEvent(MessageFolderLoadingEvent e) {
        def props = [:]
        props['core'] = core
        props['outgoing'] = false
        props['name'] = e.name
        def folder = application.mvcGroupManager.createMVCGroup('message-folder', "folder-${e.name}", props)
        view.addUserMessageFolder(folder)
    }
    
    private void refreshLibrary() {
        if (!libraryTabVisible)
            return
        if (libraryDirty) {
            libraryDirty = false
            setLoadedFiles(allSharedFiles.size())
            view.refreshSharedFiles()
        }
    }
    
    SubscriptionPOP buildSubPOP(RemoteTrustList list) {
        new SubscriptionPOP(list)
    }
    
    class SubscriptionPOP implements PersonaOrProfile {
        final RemoteTrustList trustList
        Icon icon
        SubscriptionPOP(RemoteTrustList trustList) {
            this.trustList = trustList
        }

        @Override
        Persona getPersona() {
            trustList.getPersona()
        }
        
        @Override
        Icon getThumbnail() {
            MWProfileHeader header = getHeader()
            if (header == null)
                return null
            if (icon == null)
                icon = new ThumbnailIcon(header.getThumbNail())
            return icon
        }

        @Override
        String getRawTitle() {
            getHeader()?.getTitle()
        }
        
        @Override
        MWProfileHeader getHeader() {
            core.trustService.getProfileHeader(getPersona())
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof SubscriptionPOP))
                return false
            SubscriptionPOP other = (SubscriptionPOP)o
            return trustList == other.trustList
        }
    }
}