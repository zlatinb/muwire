package com.muwire.gui

import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Path
import java.util.Calendar
import java.util.UUID

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JOptionPane
import javax.swing.JTable
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.RouterDisconnectedEvent
import com.muwire.core.SharedFile
import com.muwire.core.connection.ConnectionAttemptStatus
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.connection.DisconnectionEvent
import com.muwire.core.content.ContentControlEvent
import com.muwire.core.download.DownloadStartedEvent
import com.muwire.core.download.Downloader
import com.muwire.core.files.AllFilesLoadedEvent
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileHashingEvent
import com.muwire.core.files.FileLoadedEvent
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.search.QueryEvent
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

import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonModel
import griffon.core.env.Metadata
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.transform.FXObservable
import griffon.transform.Observable
import net.i2p.data.Base64
import net.i2p.data.Destination
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
    def downloads = []
    def uploads = []
    def shared
    def sharedTree 
    def treeRoot
    final Map<SharedFile, TreeNode> fileToNode = new HashMap<>()
    def watched = []
    def connectionList = []
    def searches = new LinkedList()
    def trusted = []
    def distrusted = []
    def subscriptions = []

    @Observable int connections
    @Observable String me
    @Observable int loadedFiles
    @Observable File hashingFile
    @Observable boolean cancelButtonEnabled
    @Observable boolean retryButtonEnabled
    @Observable boolean pauseButtonEnabled
    @Observable boolean clearButtonEnabled
    @Observable String resumeButtonText
    @Observable boolean addCommentButtonEnabled
    @Observable boolean subscribeButtonEnabled
    @Observable boolean markNeutralFromTrustedButtonEnabled
    @Observable boolean markDistrustedButtonEnabled
    @Observable boolean markNeutralFromDistrustedButtonEnabled
    @Observable boolean markTrustedButtonEnabled
    @Observable boolean reviewButtonEnabled
    @Observable boolean updateButtonEnabled
    @Observable boolean unsubscribeButtonEnabled
    
    @Observable boolean searchesPaneButtonEnabled
    @Observable boolean downloadsPaneButtonEnabled
    @Observable boolean uploadsPaneButtonEnabled
    @Observable boolean monitorPaneButtonEnabled
    @Observable boolean trustPaneButtonEnabled
    
    @Observable Downloader downloader

    private final Set<InfoHash> downloadInfoHashes = new HashSet<>()

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
        
        if (!uiSettings.sharedFilesAsTree)
            shared = []
        else {
            treeRoot = new DefaultMutableTreeNode()
            sharedTree = new DefaultTreeModel(treeRoot)
        }

        Timer timer = new Timer("download-pumper", true)
        timer.schedule({
            runInsideUIAsync {
                if (!mvcGroup.alive)
                    return

                // remove cancelled or finished downloads
                if (!clearButtonEnabled || uiSettings.clearCancelledDownloads || uiSettings.clearFinishedDownloads) {
                    def toRemove = []
                    downloads.each {
                        if (it.downloader.getCurrentState() == Downloader.DownloadState.CANCELLED) {
                            if (uiSettings.clearCancelledDownloads) {
                                toRemove << it
                            } else {
                                clearButtonEnabled = true
                            }
                        } else if (it.downloader.getCurrentState() == Downloader.DownloadState.FINISHED) {
                            if (uiSettings.clearFinishedDownloads) {
                                toRemove << it
                            } else {
                                clearButtonEnabled = true
                            }
                        }
                    }
                    toRemove.each {
                        downloads.remove(it)
                    }
                }

                builder.getVariable("uploads-table")?.model.fireTableDataChanged()

                updateTablePreservingSelection("downloads-table")
                updateTablePreservingSelection("trusted-table")
                updateTablePreservingSelection("distrusted-table")
            }
        }, 1000, 1000)

        application.addPropertyChangeListener("core", {e ->
            coreInitialized = (e.getNewValue() != null)
            core = e.getNewValue()
            routerPresent = core.router != null
            me = core.me.getHumanReadableName()
            core.eventBus.register(UIResultEvent.class, this)
            core.eventBus.register(UIResultBatchEvent.class, this)
            core.eventBus.register(DownloadStartedEvent.class, this)
            core.eventBus.register(ConnectionEvent.class, this)
            core.eventBus.register(DisconnectionEvent.class, this)
            core.eventBus.register(FileHashedEvent.class, this)
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

            core.muOptions.watchedKeywords.each {
                core.eventBus.publish(new ContentControlEvent(term : it, regex: false, add: true))
            }
            core.muOptions.watchedRegexes.each {
                core.eventBus.publish(new ContentControlEvent(term : it, regex: true, add: true))
            }
            
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

                resumeButtonText = "Retry"
                
                searchesPaneButtonEnabled = false
                downloadsPaneButtonEnabled = true
                uploadsPaneButtonEnabled = true
                monitorPaneButtonEnabled = true
                trustPaneButtonEnabled = true
            }
        })

    }

    void onAllFilesLoadedEvent(AllFilesLoadedEvent e) {
        runInsideUIAsync {
            watched.addAll(core.muOptions.watchedDirectories)
            builder.getVariable("watched-directories-table").model.fireTableDataChanged()
            watched.each { core.eventBus.publish(new FileSharedEvent(file : new File(it))) }

            core.muOptions.trustSubscriptions.each {
                core.eventBus.publish(new TrustSubscriptionEvent(persona : it, subscribe : true))
            }
        }
    }

    void onUpdateDownloadedEvent(UpdateDownloadedEvent e) {
        runInsideUIAsync {
            JOptionPane.showMessageDialog(null, "MuWire $e.version has been downloaded.  You can update now",
                "Update Downloaded", JOptionPane.INFORMATION_MESSAGE)
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
            if (!uiSettings.sharedFilesAsTree) {
                shared << e.sharedFile
                loadedFiles = shared.size()
                JTable table = builder.getVariable("shared-files-table")
                table.model.fireTableDataChanged()
            } else {
                insertIntoTree(e.sharedFile)
                loadedFiles = fileToNode.size()
            }
        }
    }

    void onFileLoadedEvent(FileLoadedEvent e) {
        runInsideUIAsync {
            if (!uiSettings.sharedFilesAsTree) {
                shared << e.loadedFile
                loadedFiles = shared.size()
                JTable table = builder.getVariable("shared-files-table")
                table.model.fireTableDataChanged()
            } else {
                insertIntoTree(e.loadedFile)
                loadedFiles = fileToNode.size()
            }
        }
    }

    void onFileUnsharedEvent(FileUnsharedEvent e) {
        runInsideUIAsync {
            if (!uiSettings.sharedFilesAsTree) {
                shared.remove(e.unsharedFile)
                loadedFiles = shared.size()
            } else {
                def dmtn = fileToNode.remove(e.unsharedFile)
                if (dmtn != null) {
                    loadedFiles = fileToNode.size()
                    while (true) {
                        def parent = dmtn.getParent()
                        parent.remove(dmtn)
                        if (parent == treeRoot)
                            break
                        if (parent.getChildCount() == 0) {
                            dmtn = parent
                            continue
                        }
                        break
                    }
                }
            }
            view.refreshSharedFiles()
        }
    }

    void onUploadEvent(UploadEvent e) {
        runInsideUIAsync {
            uploads << e.uploader
            JTable table = builder.getVariable("uploads-table")
            table.model.fireTableDataChanged()
        }
    }

    void onUploadFinishedEvent(UploadFinishedEvent e) {
        runInsideUIAsync {
            uploads.remove(e.uploader)
            JTable table = builder.getVariable("uploads-table")
            table.model.fireTableDataChanged()
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

    void onQueryEvent(QueryEvent e) {
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

            int option = JOptionPane.showConfirmDialog(null,
                "MuWire $e.version is available from $e.signer.  You have "+ metadata["application.version"]+" Update?",
                "New MuWire version availble", JOptionPane.OK_CANCEL_OPTION)
            if (option == JOptionPane.CANCEL_OPTION)
                return
            controller.search(e.infoHash,"MuWire update")
        }
    }

    void onRouterDisconnectedEvent(RouterDisconnectedEvent e) {
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
            if (!uiSettings.sharedFilesAsTree) {
                shared << e.downloadedFile
                JTable table = builder.getVariable("shared-files-table")
                table.model.fireTableDataChanged()
            } else {
                insertIntoTree(e.downloadedFile)
                loadedFiles = fileToNode.size()
            }
        }
    }
    
    private void insertIntoTree(SharedFile file) {
        Path folder = file.getFile().toPath()
        folder = folder.subpath(0, folder.getNameCount() - 1)
        TreeNode node = treeRoot
        for(Path path : folder) {
            boolean exists = false
            def children = node.children()
            def child = null
            while(children.hasMoreElements()) {
                child = children.nextElement()
                if (child.getUserObject() == path.toString()) {
                    exists = true
                    break
                }
            }
            if (!exists) {
                child = new DefaultMutableTreeNode(path.toString())
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
}