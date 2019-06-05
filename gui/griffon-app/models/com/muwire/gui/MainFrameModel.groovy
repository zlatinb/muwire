package com.muwire.gui

import java.util.concurrent.ConcurrentHashMap

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JOptionPane
import javax.swing.JTable

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.connection.ConnectionAttemptStatus
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.connection.DisconnectionEvent
import com.muwire.core.download.DownloadStartedEvent
import com.muwire.core.download.Downloader
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileLoadedEvent
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.UIResultEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustService
import com.muwire.core.update.UpdateAvailableEvent
import com.muwire.core.upload.UploadEvent
import com.muwire.core.upload.UploadFinishedEvent

import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonModel
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.transform.FXObservable
import griffon.transform.Observable
import net.i2p.data.Destination
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class MainFrameModel {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @Inject @Nonnull GriffonApplication application
    @Observable boolean coreInitialized = false
    
    def results = new ConcurrentHashMap<>()
    def downloads = []
    def uploads = []
    def shared = []
    def connectionList = []
    def searches = new LinkedList()
    def trusted = []
    def distrusted = []
    
    @Observable int connections
    @Observable String me
    @Observable boolean searchButtonsEnabled
    @Observable boolean cancelButtonEnabled
    @Observable boolean retryButtonEnabled
    
    private final Set<InfoHash> infoHashes = new HashSet<>()

    volatile Core core    

    private long lastRetryTime = System.currentTimeMillis()
        
    void updateTablePreservingSelection(String tableName) {
        def downloadTable = builder.getVariable(tableName)
        int selectedRow = downloadTable.getSelectedRow()
        downloadTable.model.fireTableDataChanged()
        downloadTable.selectionModel.setSelectionInterval(selectedRow,selectedRow)
    }
    
    void mvcGroupInit(Map<String, Object> args) {
        
        Timer timer = new Timer("download-pumper", true)
        timer.schedule({
            runInsideUIAsync {
                if (!mvcGroup.alive)
                    return
                builder.getVariable("uploads-table")?.model.fireTableDataChanged()
                
                updateTablePreservingSelection("downloads-table")
                updateTablePreservingSelection("trusted-table")
                updateTablePreservingSelection("distrusted-table")
            }
        }, 1000, 1000)

        application.addPropertyChangeListener("core", {e ->
            coreInitialized = (e.getNewValue() != null)
            core = e.getNewValue()
            me = core.me.getHumanReadableName()
            core.eventBus.register(UIResultEvent.class, this)
            core.eventBus.register(DownloadStartedEvent.class, this)
            core.eventBus.register(ConnectionEvent.class, this)
            core.eventBus.register(DisconnectionEvent.class, this)
            core.eventBus.register(FileHashedEvent.class, this)
            core.eventBus.register(FileLoadedEvent.class, this)
            core.eventBus.register(UploadEvent.class, this)
            core.eventBus.register(UploadFinishedEvent.class, this)
            core.eventBus.register(TrustEvent.class, this)
            core.eventBus.register(QueryEvent.class, this)
            core.eventBus.register(UpdateAvailableEvent.class, this)
            core.eventBus.register(FileDownloadedEvent.class, this)
            
            timer.schedule({
                int retryInterval = application.context.get("muwire-settings").downloadRetryInterval
                if (retryInterval > 0) {
                    retryInterval *= 60000
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
            }, 60000, 60000)
            
            runInsideUIAsync {
                trusted.addAll(core.trustService.good.values())
                distrusted.addAll(core.trustService.bad.values())
            }
        })
        
    }
    
    void onUIResultEvent(UIResultEvent e) {
        MVCGroup resultsGroup = results.get(e.uuid)
        resultsGroup?.model.handleResult(e)
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
            
            if (connections > 0) {
                def topPanel = builder.getVariable("top-panel")
                topPanel.getLayout().show(topPanel, "top-search-panel")
            }
            
            connectionList.add(e.endpoint.destination)
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
            
            connectionList.remove(e.destination)
            JTable table = builder.getVariable("connections-table")
            table.model.fireTableDataChanged()
        }
    }
    
    void onFileHashedEvent(FileHashedEvent e) {
        if (e.error != null)
            return // TODO do something
        if (infoHashes.contains(e.sharedFile.infoHash))
            return    
        infoHashes.add(e.sharedFile.infoHash)
        runInsideUIAsync {
            shared << e.sharedFile
            JTable table = builder.getVariable("shared-files-table")
            table.model.fireTableDataChanged()
        }
    }
    
    void onFileLoadedEvent(FileLoadedEvent e) {
        if (infoHashes.contains(e.loadedFile.infoHash))
            return
        infoHashes.add(e.loadedFile.infoHash)
        runInsideUIAsync {
            shared << e.loadedFile
            JTable table = builder.getVariable("shared-files-table")
            table.model.fireTableDataChanged()
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
            
            results.values().each { 
                it.view.pane.getClientProperty("results-table")?.model.fireTableDataChanged()
            }
        }
    }
    
    void onQueryEvent(QueryEvent e) {
        if (e.replyTo == core.me.destination)
            return
        StringBuilder sb = new StringBuilder()
        e.searchEvent.searchTerms?.each {
            sb.append(it)
            sb.append(" ")
        }
        def search = sb.toString()
        if (search.trim().size() == 0)
            return
        runInsideUIAsync {
            searches.addFirst(new IncomingSearch(search : search, replyTo : e.replyTo, originator : e.originator))
            while(searches.size() > 200)
                searches.removeLast()
            JTable table = builder.getVariable("searches-table")
            table.model.fireTableDataChanged()
        }
    }
    
    class IncomingSearch {
        String search
        Destination replyTo
        Persona originator
    }
    
    void onUpdateAvailableEvent(UpdateAvailableEvent e) {
        runInsideUIAsync {
            JOptionPane.showMessageDialog(null, "A new version of MuWire is available from $e.signer.  Please update to $e.version")
        }
    }
    
    void onFileDownloadedEvent(FileDownloadedEvent e) {
        if (!core.muOptions.shareDownloadedFiles)
            return
        infoHashes.add(e.downloadedFile.infoHash)
        runInsideUIAsync {
            shared << e.downloadedFile
            JTable table = builder.getVariable("shared-files-table")
            table.model.fireTableDataChanged()
        }
    }
}