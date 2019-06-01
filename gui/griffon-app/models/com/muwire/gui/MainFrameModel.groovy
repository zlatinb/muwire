package com.muwire.gui

import java.util.concurrent.ConcurrentHashMap

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JTable

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.connection.ConnectionAttemptStatus
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.connection.DisconnectionEvent
import com.muwire.core.download.DownloadStartedEvent
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileLoadedEvent
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.UIResultEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustService
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
    
    @Observable int connections
    @Observable String me
    @Observable boolean searchButtonsEnabled
    
    private final Set<InfoHash> infoHashes = new HashSet<>()

    volatile Core core    
    
    void mvcGroupInit(Map<String, Object> args) {
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
        })
        Timer timer = new Timer("download-pumper", true)
        timer.schedule({
            runInsideUIAsync {
                if (!mvcGroup.alive)
                    return
                builder.getVariable("downloads-table")?.model.fireTableDataChanged()
                builder.getVariable("uploads-table")?.model.fireTableDataChanged()
            }
        }, 1000, 1000)
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
            
            connectionList.add(e.endpoint.destination)
            JTable table = builder.getVariable("connections-table")
            table.model.fireTableDataChanged()
        }
    }
    
    void onDisconnectionEvent(DisconnectionEvent e) {
        runInsideUIAsync {
            connections = core.connectionManager.getConnections().size()
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
            JTable table = builder.getVariable("results-table")
            table.model.fireTableDataChanged()
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
            searches.addFirst(search)
            while(searches.size() > 200)
                searches.removeLast()
            JTable table = builder.getVariable("searches-table")
            table.model.fireTableDataChanged()
        }
    }
}