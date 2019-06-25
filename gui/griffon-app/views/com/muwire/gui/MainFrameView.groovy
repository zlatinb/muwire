package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.core.env.Metadata
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64
import net.i2p.data.DataHelper

import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.border.Border
import javax.swing.table.DefaultTableCellRenderer

import com.muwire.core.Constants
import com.muwire.core.download.Downloader
import com.muwire.core.files.FileSharedEvent

import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.charset.StandardCharsets

import javax.annotation.Nonnull
import javax.inject.Inject

@ArtifactProviderFor(GriffonView)
class MainFrameView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    MainFrameModel model
    
    @Inject Metadata metadata
    
    def downloadsTable
    def lastDownloadSortEvent
    def lastSharedSortEvent
    
    void initUI() {
        UISettings settings = application.context.get("ui-settings")
        builder.with {
            application(size : [1024,768], id: 'main-frame',
            locationRelativeTo : null,
            title: application.configuration['application.title'] + " " + 
                metadata["application.version"] + " revision " + metadata["build.revision"],
            iconImage:   imageIcon('/MuWire-48x48.png').image,
            iconImages: [imageIcon('/MuWire-48x48.png').image,
                imageIcon('/MuWire-32x32.png').image,
                imageIcon('/MuWire-16x16.png').image],
            pack : false,
            visible : bind { model.coreInitialized }) {
                menuBar {
                    menu (text : "Options") {
                        menuItem("Configuration", actionPerformed : {mvcGroup.createMVCGroup("Options")})
                    }
                }
                borderLayout()
                panel (border: etchedBorder(), constraints : BorderLayout.NORTH) {
                    borderLayout()
                    panel (constraints: BorderLayout.WEST) {
                        gridLayout(rows:1, cols: 2)
                        button(text: "Searches", actionPerformed : showSearchWindow)
                        button(text: "Uploads", actionPerformed : showUploadsWindow)
                        if (settings.showMonitor)
                            button(text: "Monitor", actionPerformed : showMonitorWindow)
                        button(text: "Trust", actionPerformed : showTrustWindow)
                    }
                    panel(id: "top-panel", constraints: BorderLayout.CENTER) {
                        cardLayout()
                        label(constraints : "top-connect-panel",
                            text : "        MuWire is connecting, please wait.  You will be able to search soon.") // TODO: real padding
                        panel(constraints : "top-search-panel") {
                            borderLayout()
                            panel(constraints: BorderLayout.CENTER) {
                                borderLayout()
                                label("        Enter search here:", constraints: BorderLayout.WEST) // TODO: fix this
                                textField(id: "search-field", constraints: BorderLayout.CENTER, action : searchAction)

                            }
                            panel( constraints: BorderLayout.EAST) {
                                button(text: "Search", searchAction)
                            }
                        }
                    }
                }
                panel (id: "cards-panel", constraints : BorderLayout.CENTER) {
                    cardLayout()
                    panel (constraints : "search window") {
                        borderLayout()
                        splitPane( orientation : JSplitPane.VERTICAL_SPLIT, dividerLocation : 500,
                        continuousLayout : true, constraints : BorderLayout.CENTER) {
                            panel (constraints : JSplitPane.TOP) {
                                borderLayout()
                                tabbedPane(id : "result-tabs", constraints: BorderLayout.CENTER)
                                panel(constraints : BorderLayout.SOUTH) {
                                    button(text : "Download", enabled : bind {model.downloadActionEnabled}, downloadAction)
                                    button(text : "Trust", enabled: bind {model.trustButtonsEnabled }, trustAction)
                                    button(text : "Distrust", enabled : bind {model.trustButtonsEnabled}, distrustAction)
                                }
                            }
                            panel (constraints : JSplitPane.BOTTOM) {
                                borderLayout()
                                scrollPane (constraints : BorderLayout.CENTER) {
                                    downloadsTable = table(id : "downloads-table", autoCreateRowSorter : true) {
                                        tableModel(list: model.downloads) {
                                            closureColumn(header: "Name", preferredWidth: 350, type: String, read : {row -> row.downloader.file.getName()})
                                            closureColumn(header: "Status", preferredWidth: 50, type: String, read : {row -> row.downloader.getCurrentState().toString()})
                                            closureColumn(header: "Progress", preferredWidth: 20, type: String, read: { row ->
                                                int pieces = row.downloader.nPieces
                                                int done = row.downloader.donePieces()
                                                "$done/$pieces pieces".toString()
                                            })
                                            closureColumn(header: "Sources", preferredWidth : 10, type: Integer, read : {row -> row.downloader.activeWorkers()})
                                            closureColumn(header: "Speed", preferredWidth: 50, type:String, read :{row -> 
                                                DataHelper.formatSize2Decimal(row.downloader.speed(), false) + "B/sec"
                                            })
                                        }
                                    }
                                }
                                panel (constraints : BorderLayout.SOUTH) {
                                    button(text: "Pause", enabled : bind {model.pauseButtonEnabled}, pauseAction)
                                    button(text: "Cancel", enabled : bind {model.cancelButtonEnabled }, cancelAction )
                                    button(text: bind { model.resumeButtonText }, enabled : bind {model.retryButtonEnabled}, resumeAction)
                                }
                            }
                        }
                    }
                    panel (constraints: "uploads window"){
                        gridLayout(cols : 1, rows : 2)
                        panel {
                            gridLayout(cols : 2, rows : 1) 
                            panel {
                                borderLayout()
                                panel (constraints : BorderLayout.NORTH) {
                                    button(text : "Add directories to watch", actionPerformed : watchDirectories)
                                }
                                scrollPane (constraints : BorderLayout.CENTER) {
                                    table(id : "watched-directories-table", autoCreateRowSorter: true) {
                                        tableModel(list : model.watched) {
                                            closureColumn(header: "Watched Directories", type : String, read : { it })
                                        }
                                    }
                                }
                            }
                            panel {
                                borderLayout()
                                panel (constraints : BorderLayout.NORTH) {
                                    button(text : "Share files", actionPerformed : shareFiles)
                                }
                                scrollPane(constraints : BorderLayout.CENTER) {
                                    table(id : "shared-files-table", autoCreateRowSorter: true) {
                                        tableModel(list : model.shared) {
                                            closureColumn(header : "Name", preferredWidth : 500, type : String, read : {row -> row.file.getAbsolutePath()})
                                            closureColumn(header : "Size", preferredWidth : 100, type : Long, read : {row -> row.file.length() })
                                        }
                                    }
                                }
                            }
                        }
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH){
                                label("Uploads")
                            }
                            scrollPane (constraints : BorderLayout.CENTER) {
                                table(id : "uploads-table") {
                                    tableModel(list : model.uploads) {
                                        closureColumn(header : "Name", type : String, read : {row -> row.getName() })
                                        closureColumn(header : "Progress", type : String, read : { row ->
                                            int percent = row.getProgress()
                                            "$percent% of piece".toString()
                                        })
                                        closureColumn(header : "Downloader", type : String, read : { row -> 
                                            row.getDownloader()
                                        })
                                        closureColumn(header : "Remote Pieces", type : String, read : { row ->
                                            "${row.getDonePieces()}/${row.getTotalPieces()}".toString()
                                        })
                                    }
                                }
                            }
                        }
                    }
                    panel (constraints: "monitor window") {
                        gridLayout(rows : 1, cols : 2)
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH){
                                label("Connections")
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "connections-table") {
                                    tableModel(list : model.connectionList) {
                                        closureColumn(header : "Destination", preferredWidth: 250, type: String, read : { row -> row.destination.toBase32() })
                                        closureColumn(header : "Direction", preferredWidth: 20, type: String, read : { row ->
                                            if (row.incoming)
                                                return "In"
                                            else 
                                                return "Out"
                                        })
                                    }
                                }
                            }
                        }
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH){
                                label("Incoming searches")
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "searches-table") {
                                    tableModel(list : model.searches) {
                                        closureColumn(header : "Keywords", type : String, read : { 
                                            sanitized = it.search.replace('<', ' ')
                                            sanitized 
                                        })
                                        closureColumn(header : "From", type : String, read : {
                                            if (it.originator != null) {
                                                return it.originator.getHumanReadableName()
                                            } else {
                                                return it.replyTo.toBase32()
                                            }
                                        })
                                    }
                                }
                            }
                        }
                    }
                    panel(constraints : "trust window") {
                        gridLayout(rows: 1, cols :2)
                        panel (border : etchedBorder()){
                            borderLayout()
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "trusted-table", autoCreateRowSorter : true) {
                                    tableModel(list : model.trusted) {
                                        closureColumn(header : "Trusted Users", type : String, read : { it.getHumanReadableName() } )
                                    }
                                }
                            }
                            panel (constraints : BorderLayout.EAST) {
                                gridBagLayout()
                                button(text : "Mark Neutral", constraints : gbc(gridx: 0, gridy: 0), markNeutralFromTrustedAction)
                                button(text : "Mark Distrusted", constraints : gbc(gridx: 0, gridy:1), markDistrustedAction)
                            }
                        }
                        panel (border : etchedBorder()){
                            borderLayout()
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "distrusted-table", autoCreateRowSorter : true) {
                                    tableModel(list : model.distrusted) {
                                        closureColumn(header: "Distrusted Users", type : String, read : { it.getHumanReadableName() } )
                                    }
                                }
                            }
                            panel(constraints : BorderLayout.WEST) {
                                gridBagLayout()
                                button(text: "Mark Neutral", constraints: gbc(gridx: 0, gridy: 0), markNeutralFromDistrustedAction)
                                button(text: "Mark Trusted", constraints : gbc(gridx: 0, gridy : 1), markTrustedAction)
                            }
                        }
                    }
                }
                panel (border: etchedBorder(), constraints : BorderLayout.SOUTH) {
                    borderLayout()
                    label(text : bind {model.me}, constraints: BorderLayout.CENTER)
                    panel (constraints : BorderLayout.EAST) {
                        label("Connections:")
                        label(text : bind {model.connections})
                    }
                }

            }
        }
    }
    
    void mvcGroupInit(Map<String, String> args) {
        def downloadsTable = builder.getVariable("downloads-table")
        def selectionModel = downloadsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = selectedDownloaderRow()
            if (selectedRow < 0) {
                model.cancelButtonEnabled = false
                model.retryButtonEnabled = false
                model.pauseButtonEnabled = false
                return
            }
            def downloader = model.downloads[selectedRow]?.downloader
            if (downloader == null)
                return
            switch(downloader.getCurrentState()) {
                case Downloader.DownloadState.CONNECTING :
                case Downloader.DownloadState.DOWNLOADING :
                case Downloader.DownloadState.HASHLIST:
                model.cancelButtonEnabled = true
                model.pauseButtonEnabled = true
                model.retryButtonEnabled = false
                break
                case Downloader.DownloadState.FAILED:
                model.cancelButtonEnabled = true
                model.retryButtonEnabled = true
                model.resumeButtonText = "Retry"
                model.pauseButtonEnabled = false
                break
                case Downloader.DownloadState.PAUSED:
                model.cancelButtonEnabled = true
                model.retryButtonEnabled = true
                model.resumeButtonText = "Resume"
                model.pauseButtonEnabled = false
                break
                default:
                model.cancelButtonEnabled = false
                model.retryButtonEnabled = false
                model.pauseButtonEnabled = false
            }
        })
        
        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        downloadsTable.setDefaultRenderer(Integer.class, centerRenderer)
        
        downloadsTable.rowSorter.addRowSorterListener({evt -> lastDownloadSortEvent = evt})
        downloadsTable.rowSorter.setSortsOnUpdates(true)
        
        downloadsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showDownloadsMenu(e)
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showDownloadsMenu(e)
            }
        })
        
        // shared files table
        def sharedFilesTable = builder.getVariable("shared-files-table")
        sharedFilesTable.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())
        
        sharedFilesTable.rowSorter.addRowSorterListener({evt -> lastSharedSortEvent = evt})
        sharedFilesTable.rowSorter.setSortsOnUpdates(true)
        
        JPopupMenu sharedFilesMenu = new JPopupMenu()
//        JMenuItem unshareSelectedFiles = new JMenuItem("Unshare selected files")
//        unshareSelectedFiles.addActionListener({mvcGroup.controller.unshareSelectedFiles()})
        JMenuItem copyHashToClipboard = new JMenuItem("Copy hash to clipboard")
        copyHashToClipboard.addActionListener({mvcGroup.view.copyHashToClipboard(sharedFilesTable)})
        sharedFilesMenu.add(copyHashToClipboard)
        sharedFilesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showPopupMenu(sharedFilesMenu, e)
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showPopupMenu(sharedFilesMenu, e)
            }
        })
        
        // searches table
        def searchesTable = builder.getVariable("searches-table")
        JPopupMenu searchTableMenu = new JPopupMenu()
        JMenuItem copySearchToClipboard = new JMenuItem("Copy search to clipboard")
        copySearchToClipboard.addActionListener({mvcGroup.view.copySearchToClipboard(searchesTable)})
        searchTableMenu.add(copySearchToClipboard)
        searchesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showPopupMenu(searchTableMenu, e)
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showPopupMenu(searchTableMenu, e)
            }
        })
        
    }
    
    private static void showPopupMenu(JPopupMenu menu, MouseEvent event) {
        menu.show(event.getComponent(), event.getX(), event.getY())
    }
    
    def copyHashToClipboard(JTable sharedFilesTable) {
        int selected = sharedFilesTable.getSelectedRow()
        if (selected < 0)
            return
        if (lastSharedSortEvent != null) 
            selected = sharedFilesTable.rowSorter.convertRowIndexToModel(selected)
        String root = Base64.encode(model.shared[selected].infoHash.getRoot())
        StringSelection selection = new StringSelection(root)
        def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
        clipboard.setContents(selection, null)
    }
    
    def copySearchToClipboard(JTable searchesTable) {
        int selected = searchesTable.getSelectedRow()
        if (selected < 0)
            return
        String search = model.searches[selected].search
        StringSelection selection = new StringSelection(search)
        def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
        clipboard.setContents(selection, null)
    }

    int selectedDownloaderRow() {
        int selected = builder.getVariable("downloads-table").getSelectedRow()
        if (lastDownloadSortEvent != null)
            selected = lastDownloadSortEvent.convertPreviousRowIndexToModel(selected)
        selected
    }
    
    def showDownloadsMenu(MouseEvent e) {
        int selected = selectedDownloaderRow()
        if (selected < 0)
            return
        boolean pauseEnabled = false
        boolean cancelEnabled = false
        boolean retryEnabled = false
        String resumeText = "Retry"
        Downloader downloader = model.downloads[selected].downloader
        switch(downloader.currentState) {
            case Downloader.DownloadState.DOWNLOADING:
            case Downloader.DownloadState.HASHLIST:
            case Downloader.DownloadState.CONNECTING:
                pauseEnabled = true
                cancelEnabled = true
                retryEnabled = false
                break
            case Downloader.DownloadState.FAILED:
                pauseEnabled = false
                cancelEnabled = true
                retryEnabled = true
                break
            case Downloader.DownloadState.PAUSED:
                pauseEnabled = false
                cancelEnabled = true
                retryEnabled = true
                resumeText = "Resume"
                break
            default :
                pauseEnabled = false
                cancelEnabled = false
                retryEnabled = false
        }
        
        JPopupMenu menu = new JPopupMenu()
        JMenuItem copyHashToClipboard = new JMenuItem("Copy hash to clipboard")
        copyHashToClipboard.addActionListener({
            String hash = Base64.encode(downloader.infoHash.getRoot())
            StringSelection selection = new StringSelection(hash)
            def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
            clipboard.setContents(selection, null)
        })
        menu.add(copyHashToClipboard)
        
        if (pauseEnabled) {
            JMenuItem pause = new JMenuItem("Pause")
            pause.addActionListener({mvcGroup.controller.pause()})
            menu.add(pause)
        }
        
        if (cancelEnabled) {
            JMenuItem cancel = new JMenuItem("Cancel")
            cancel.addActionListener({mvcGroup.controller.cancel()})
            menu.add(cancel)
        }
        
        if (retryEnabled) {
            JMenuItem retry = new JMenuItem(resumeText)
            retry.addActionListener({mvcGroup.controller.resume()})
            menu.add(retry)
        }
        
        showPopupMenu(menu, e)
    }
    
    def showSearchWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "search window")
    }

    def showUploadsWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "uploads window")
    }
    
    def showMonitorWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel,"monitor window")
    }
    
    def showTrustWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel,"trust window")
    }
    
    def shareFiles = {
        def chooser = new JFileChooser()
        chooser.setFileHidingEnabled(false)
        chooser.setDialogTitle("Select file to share")
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY)
        int rv = chooser.showOpenDialog(null)
        if (rv == JFileChooser.APPROVE_OPTION) {
            model.core.eventBus.publish(new FileSharedEvent(file : chooser.getSelectedFile()))
        }
    }
    
    def watchDirectories = {
        def chooser = new JFileChooser()
        chooser.setFileHidingEnabled(false)
        chooser.setDialogTitle("Select directory to watch")
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        int rv = chooser.showOpenDialog(null)
        if (rv == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile()
            model.watched << f.getAbsolutePath()
            application.context.get("muwire-settings").watchedDirectories << f.getAbsolutePath()
            mvcGroup.controller.saveMuWireSettings()
            builder.getVariable("watched-directories-table").model.fireTableDataChanged()
            model.core.eventBus.publish(new FileSharedEvent(file : f))
        }
    }
}