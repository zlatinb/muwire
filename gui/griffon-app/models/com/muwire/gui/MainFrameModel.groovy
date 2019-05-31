package com.muwire.gui

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
import com.muwire.core.search.UIResultEvent

import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.FXObservable
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class MainFrameModel {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @Inject @Nonnull GriffonApplication application
    @Observable boolean coreInitialized = false
    
    @Observable def results = []
    @Observable def downloads = []
    @Observable def shared = []
    @Observable int connections
    @Observable String me
    
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
        })
        Timer timer = new Timer("download-pumper", true)
        timer.schedule({
            runInsideUIAsync {
                builder.getVariable("downloads-table").model.fireTableDataChanged()
            }
        }, 1000, 1000)
    }
    
    void onUIResultEvent(UIResultEvent e) {
        runInsideUIAsync {
            results << e
            JTable table = builder.getVariable("results-table")
            table.model.fireTableDataChanged()
        }
    }
    
    void onDownloadStartedEvent(DownloadStartedEvent e) {
        runInsideUIAsync {
            downloads << e
        }
    }
    
    void onConnectionEvent(ConnectionEvent e) {
        runInsideUIAsync {
            connections = core.connectionManager.getConnections().size()
        }
    }
    
    void onDisconnectionEvent(DisconnectionEvent e) {
        runInsideUIAsync {
            connections = core.connectionManager.getConnections().size()
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
}