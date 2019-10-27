package com.muwire.clilanterna

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.BorderLayout
import com.googlecode.lanterna.gui2.Borders
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.Panels
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.gui2.TextBox
import com.muwire.core.Core
import com.muwire.core.DownloadedFile
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileLoadedEvent
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.hostcache.HostDiscoveredEvent
import com.muwire.core.update.UpdateAvailableEvent
import com.muwire.core.update.UpdateDownloadedEvent

import net.i2p.data.Base64

class MainWindowView extends BasicWindow {
    
    private final Core core
    private final TextGUI textGUI
    private final Screen screen

    private final TextBox searchTextBox
    
    private final DownloadsModel downloadsModel
    private final UploadsModel uploadsModel
    private final FilesModel filesModel
    private final TrustModel trustModel
    
    private final Label connectionCount, incoming, outgoing
    private final Label known, failing, hopeless
    private final Label sharedFiles
    private final Label updateStatus
    
    public MainWindowView(String title, Core core, TextGUI textGUI, Screen screen, CliSettings props) {
        super(title);
        
        this.core = core
        this.textGUI = textGUI
        this.screen = screen
        
        downloadsModel = new DownloadsModel(textGUI.getGUIThread(),core, props)
        uploadsModel = new UploadsModel(textGUI.getGUIThread(), core, props)
        filesModel = new FilesModel(textGUI.getGUIThread(),core)
        trustModel = new TrustModel(textGUI.getGUIThread(), core)
        
        setHints([Window.Hint.EXPANDED])
        Panel contentPanel = new Panel()
        setComponent(contentPanel)
        
        BorderLayout borderLayout = new BorderLayout()
        contentPanel.setLayoutManager(borderLayout)
        
        Panel buttonsPanel = new Panel()
        contentPanel.addComponent(buttonsPanel, BorderLayout.Location.TOP)
        
        GridLayout gridLayout = new GridLayout(7)
        buttonsPanel.setLayoutManager(gridLayout)
        
        searchTextBox = new TextBox(new TerminalSize(40, 1))
        Button searchButton = new Button("Search", { search() })
        Button downloadsButton = new Button("Downloads", {download()})
        Button uploadsButton = new Button("Uploads", {upload()})
        Button filesButton = new Button("Files", { files() })
        Button trustButton = new Button("Trust", {trust()})
        Button quitButton = new Button("Quit", {close()})
        
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        
        buttonsPanel.with { 
            addComponent(searchTextBox, layoutData)
            addComponent(searchButton, layoutData)
            addComponent(downloadsButton, layoutData)
            addComponent(uploadsButton, layoutData)
            addComponent(filesButton, layoutData)
            addComponent(trustButton, layoutData)
            addComponent(quitButton, layoutData)
        }
        
        Panel bottomPanel = new Panel()
        contentPanel.addComponent(bottomPanel, BorderLayout.Location.BOTTOM)
        BorderLayout bottomLayout = new BorderLayout()
        bottomPanel.setLayoutManager(bottomLayout)
        
        Label persona = new Label(core.me.getHumanReadableName())
        bottomPanel.addComponent(persona, BorderLayout.Location.LEFT)
        
        
        Panel connectionsPanel = new Panel()
        connectionsPanel.setLayoutManager(new GridLayout(2))
        Label connections = new Label("Connections:")
        connectionsPanel.addComponent(connections, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        connectionCount = new Label("0")
        connectionsPanel.addComponent(connectionCount, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        
        bottomPanel.addComponent(connectionsPanel, BorderLayout.Location.RIGHT)

        
        Panel centralPanel = new Panel()
        centralPanel.setLayoutManager(new GridLayout(1))
        contentPanel.addComponent(centralPanel, BorderLayout.Location.CENTER)
        Panel statusPanel = new Panel()
        statusPanel.setLayoutManager(new GridLayout(2))
        statusPanel.withBorder(Borders.doubleLine("Stats"))
        centralPanel.addComponent(statusPanel, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER, true, true))

        incoming = new Label("0")
        outgoing = new Label("0")
        known = new Label("0")
        failing = new Label("0")
        hopeless = new Label("0")
        sharedFiles = new Label("0")
        updateStatus = new Label("Unknown")
                
        statusPanel.with { 
            addComponent(new Label("Incoming Connections: "), layoutData)
            addComponent(incoming, layoutData)
            addComponent(new Label("Outgoing Connections: "), layoutData)
            addComponent(outgoing, layoutData)
            addComponent(new Label("Known Hosts: "), layoutData)
            addComponent(known, layoutData)
            addComponent(new Label("Failing Hosts: "), layoutData)
            addComponent(failing, layoutData)
            addComponent(new Label("Hopeless Hosts: "), layoutData)
            addComponent(hopeless, layoutData)
            addComponent(new Label("Shared Files: "), layoutData)
            addComponent(sharedFiles, layoutData)
            addComponent(new Label("Update Status: "), layoutData)
            addComponent(updateStatus, layoutData)
        }
        
        refreshStats()
        
        searchButton.takeFocus()
        core.eventBus.register(ConnectionEvent.class, this)
        core.eventBus.register(HostDiscoveredEvent.class, this)
        core.eventBus.register(FileLoadedEvent.class, this)
        core.eventBus.register(FileHashedEvent.class, this)
        core.eventBus.register(FileUnsharedEvent.class, this)
        core.eventBus.register(FileDownloadedEvent.class, this)
        core.eventBus.register(UpdateAvailableEvent.class, this)
        core.eventBus.register(UpdateDownloadedEvent.class, this)
    }
    
    void onConnectionEvent(ConnectionEvent e) {
        textGUI.getGUIThread().invokeLater {
            connectionCount.setText(String.valueOf(core.connectionManager.connections.size()))
            refreshStats()
        }
    }
    
    void onHostDiscoveredEvent(HostDiscoveredEvent e) {
        textGUI.getGUIThread().invokeLater {
            refreshStats()
        }
    }
    
    void onFileLoadedEvent(FileLoadedEvent e) {
        textGUI.getGUIThread().invokeLater {
            refreshStats()
        }
    }
    
    void onFileHashedEvent(FileHashedEvent e) {
        textGUI.getGUIThread().invokeLater {
            refreshStats()
        }
    }
    
    void onFileUnsharedEvent(FileUnsharedEvent e) {
        textGUI.getGUIThread().invokeLater {
            refreshStats()
        }
    }
    
    void onFileDownloadedEvent(FileDownloadedEvent e) {
        textGUI.getGUIThread().invokeLater {
            refreshStats()
        }
    }
    
    void onUpdateAvailableEvent(UpdateAvailableEvent e) {
        textGUI.getGUIThread().invokeLater {
            String label = "$e.version is available with hash $e.infoHash"
            updateStatus.setText(label)
            String message = "Version $e.version is available from $e.signer, search for $e.infoHash"
            MessageDialog.showMessageDialog(textGUI, "Update Available", message, MessageDialogButton.OK)
        }
    }
    
    void onUpdateDownloadedEvent(UpdateDownloadedEvent e) {
        textGUI.getGUIThread().invokeLater {
            String label = "$e.version downloaded"
            updateStatus.setText(label)
            String message = "Version $e.version from $e.signer has been downloaded.  You can update now."
            MessageDialog.showMessageDialog(textGUI, "Update Available", message, MessageDialogButton.OK)
        }
    }
    
    private TerminalSize sizeForTables() {
        TerminalSize full = screen.getTerminalSize()
        return new TerminalSize(full.getColumns(), full.getRows() - 10)
    }
    
    private void search() {
        String query = searchTextBox.getText()
        query = query.trim()
        if (query.length() == 0)
            return
        if (query.length() > 128)
            query = query.substring(0, 128)
        
        SearchModel model = new SearchModel(query, core, textGUI.getGUIThread())
        textGUI.addWindowAndWait(new SearchView(model,core, textGUI, sizeForTables()))
    }
    
    
    private void download() {
        textGUI.addWindowAndWait(new DownloadsView(core, downloadsModel, textGUI, sizeForTables()))
    }
    
    private void upload() {
        textGUI.addWindowAndWait(new UploadsView(uploadsModel, sizeForTables()))
    }
    
    private void files() {
        textGUI.addWindowAndWait(new FilesView(filesModel, textGUI, core, sizeForTables()))
    }
    
    private void trust() {
        textGUI.addWindowAndWait(new TrustView(trustModel, textGUI, core, sizeForTables()))
    }
    
    private void refreshStats() {
        int inCon = 0
        int outCon = 0
        core.connectionManager.getConnections().each { 
            if (it.isIncoming())
                inCon++
            else
                outCon++
        }
        int knownHosts = core.hostCache.hosts.size()
        int failingHosts = core.hostCache.countFailingHosts()
        int hopelessHosts = core.hostCache.countHopelessHosts()
        int shared = core.fileManager.fileToSharedFile.size()
        
        incoming.setText(String.valueOf(inCon))
        outgoing.setText(String.valueOf(outCon))
        known.setText(String.valueOf(knownHosts))
        failing.setText(String.valueOf(failingHosts))
        hopeless.setText(String.valueOf(hopelessHosts))
        sharedFiles.setText(String.valueOf(shared))
    }
}
