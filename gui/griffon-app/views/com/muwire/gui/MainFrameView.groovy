package com.muwire.gui

import griffon.core.GriffonApplication
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
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.TransferHandler
import javax.swing.border.Border
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

import com.muwire.core.Constants
import com.muwire.core.Core
import com.muwire.core.MuWireSettings
import com.muwire.core.SharedFile
import com.muwire.core.download.Downloader
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.trust.RemoteTrustList
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.charset.StandardCharsets

import javax.annotation.Nonnull
import javax.inject.Inject

@ArtifactProviderFor(GriffonView)
class MainFrameView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    MainFrameModel model

    @Inject @Nonnull GriffonApplication application
    @Inject Metadata metadata

    def downloadsTable
    def lastDownloadSortEvent
    def lastSharedSortEvent
    def trustTablesSortEvents = [:]

    UISettings settings
    
    void initUI() {
        settings = application.context.get("ui-settings")
        builder.with {
            application(size : [1024,768], id: 'main-frame',
            locationRelativeTo : null,
            defaultCloseOperation : JFrame.HIDE_ON_CLOSE,
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
                        menuItem("Content Control", actionPerformed : {
                            def env = [:]
                            env["core"] = model.core
                            mvcGroup.createMVCGroup("content-panel", env)
                        })
                    }
                    menu (text : "Status") {
                        menuItem("MuWire", actionPerformed : {mvcGroup.createMVCGroup("mu-wire-status")})
                        MuWireSettings muSettings = application.context.get("muwire-settings")
                        menuItem("I2P", enabled : bind {model.routerPresent}, actionPerformed: {mvcGroup.createMVCGroup("i-2-p-status")})
                    }
                }
                borderLayout()
                panel (border: etchedBorder(), constraints : BorderLayout.NORTH) {
                    borderLayout()
                    panel (constraints: BorderLayout.WEST) {
                        gridLayout(rows:1, cols: 2)
                        button(text: "Searches", enabled : bind{model.searchesPaneButtonEnabled},actionPerformed : showSearchWindow)
                        button(text: "Downloads", enabled : bind{model.downloadsPaneButtonEnabled}, actionPerformed : showDownloadsWindow)
                        button(text: "Uploads", enabled : bind{model.uploadsPaneButtonEnabled}, actionPerformed : showUploadsWindow)
                        if (settings.showMonitor)
                            button(text: "Monitor", enabled: bind{model.monitorPaneButtonEnabled},actionPerformed : showMonitorWindow)
                        button(text: "Trust", enabled:bind{model.trustPaneButtonEnabled},actionPerformed : showTrustWindow)
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
                        tabbedPane(id : "result-tabs", constraints: BorderLayout.CENTER)
                    }
                    panel (constraints: "downloads window") {
                        gridLayout(rows : 1, cols : 1)
                        splitPane(orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout : true, dividerLocation: 500 ) {
                            panel {
                                borderLayout()
                                scrollPane (constraints : BorderLayout.CENTER) {
                                    downloadsTable = table(id : "downloads-table", autoCreateRowSorter : true) {
                                        tableModel(list: model.downloads) {
                                            closureColumn(header: "Name", preferredWidth: 300, type: String, read : {row -> row.downloader.file.getName()})
                                            closureColumn(header: "Status", preferredWidth: 50, type: String, read : {row -> row.downloader.getCurrentState().toString()})
                                            closureColumn(header: "Progress", preferredWidth: 70, type: Downloader, read: { row -> row.downloader })
                                            closureColumn(header: "Speed", preferredWidth: 50, type:String, read :{row ->
                                                DataHelper.formatSize2Decimal(row.downloader.speed(), false) + "B/sec"
                                            })
                                            closureColumn(header : "ETA", preferredWidth : 50, type:String, read :{ row ->
                                                def speed = row.downloader.speed()
                                                if (speed == 0)
                                                    return "Unknown"
                                                else {
                                                    def remaining = (row.downloader.nPieces - row.downloader.donePieces()) * row.downloader.pieceSize / speed
                                                    return DataHelper.formatDuration(remaining.toLong() * 1000)
                                                }
                                            })
                                        }
                                    }
                                }
                                panel (constraints : BorderLayout.SOUTH) {
                                    button(text: "Pause", enabled : bind {model.pauseButtonEnabled}, pauseAction)
                                    button(text: bind { model.resumeButtonText }, enabled : bind {model.retryButtonEnabled}, resumeAction)
                                    button(text: "Cancel", enabled : bind {model.cancelButtonEnabled }, cancelAction)
                                    button(text: "Clear Done", enabled : bind {model.clearButtonEnabled}, clearAction)
                                }
                            }
                            panel {
                                borderLayout()
                                panel(constraints : BorderLayout.NORTH) {
                                    label(text : "Download Details")
                                }
                                scrollPane(constraints : BorderLayout.CENTER) {
                                    panel (id : "download-details-panel") {
                                        cardLayout()
                                        panel (constraints : "select-download") {
                                            label(text : "Select a download to view details")
                                        }
                                        panel(constraints : "download-selected") {
                                            gridBagLayout()
                                            label(text : "Download Location:", constraints : gbc(gridx:0, gridy:0))
                                            label(text : bind {model.downloader?.file?.getAbsolutePath()},
                                            constraints: gbc(gridx:1, gridy:0, gridwidth: 2, insets : [0,0,0,20]))
                                            label(text : "Piece Size", constraints : gbc(gridx: 0, gridy:1))
                                            label(text : bind {model.downloader?.pieceSize}, constraints : gbc(gridx:1, gridy:1))
                                            label(text : "Known Sources:", constraints : gbc(gridx:3, gridy: 0))
                                            label(text : bind {model.downloader?.activeWorkers?.size()}, constraints : gbc(gridx:4, gridy:0, insets : [0,0,0,20]))
                                            label(text : "Active Sources:", constraints : gbc(gridx:3, gridy:1))
                                            label(text : bind {model.downloader?.activeWorkers()}, constraints : gbc(gridx:4, gridy:1, insets : [0,0,0,20]))
                                            label(text : "Total Pieces:", constraints : gbc(gridx:5, gridy: 0))
                                            label(text : bind {model.downloader?.nPieces}, constraints : gbc(gridx:6, gridy:0, insets : [0,0,0,20]))
                                            label(text : "Done Pieces:", constraints: gbc(gridx:5, gridy: 1))
                                            label(text : bind {model.downloader?.donePieces()}, constraints : gbc(gridx:6, gridy:1, insets : [0,0,0,20]))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    panel (constraints: "uploads window"){
                        gridLayout(cols : 1, rows : 2)
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH) {
                                label(text : bind {
                                    if (model.hashingFile == null) {
                                        "You can drag-and-drop files and directories here"
                                    } else {
                                        "hashing: " + model.hashingFile.getAbsolutePath() + " (" + DataHelper.formatSize2Decimal(model.hashingFile.length(), false).toString() + "B)"
                                    }
                                })
                            }
                            panel (border : etchedBorder(), constraints : BorderLayout.CENTER) {
                                borderLayout()
                                panel (id : "shared-files-panel", constraints : BorderLayout.CENTER){
                                    cardLayout()
                                    panel (constraints : "shared files table") {
                                        borderLayout()
                                        scrollPane(constraints : BorderLayout.CENTER) {
                                            table(id : "shared-files-table", autoCreateRowSorter: true) {
                                                tableModel(list : model.shared) {
                                                    closureColumn(header : "Name", preferredWidth : 500, type : String, read : {row -> row.getCachedPath()})
                                                    closureColumn(header : "Size", preferredWidth : 100, type : Long, read : {row -> row.getCachedLength() })
                                                    closureColumn(header : "Comments", preferredWidth : 100, type : Boolean, read : {it.getComment() != null})
                                                }
                                            }
                                        }
                                    }
                                    panel (constraints : "shared files tree") {
                                        borderLayout()
                                        scrollPane(constraints : BorderLayout.CENTER) {
                                            def jtree = new JTree(model.sharedTree)
                                            jtree.setCellRenderer(new SharedTreeRenderer())
                                            tree(id : "shared-files-tree", rootVisible : false, expandsSelectedPaths: true, jtree)
                                        }
                                    }
                                }
                            }
                            panel (constraints : BorderLayout.SOUTH) {
                                gridLayout(rows:1, cols:3)
                                panel {
                                    buttonGroup(id : "sharedViewType")
                                    radioButton(text : "Tree", selected : true, buttonGroup : sharedViewType, actionPerformed : showSharedFilesTree)
                                    radioButton(text : "Table", selected : false, buttonGroup : sharedViewType, actionPerformed : showSharedFilesTable)
                                }
                                panel {
                                    button(text : "Share files", actionPerformed : shareFiles)
                                    button(text : "Add Comment", enabled : bind {model.addCommentButtonEnabled}, addCommentAction)
                                }
                                panel {
                                    panel {
                                        label("Shared:")
                                        label(text : bind {model.loadedFiles}, id : "shared-files-count")
                                    }
                                }
                            }
                        }
                        panel (border : etchedBorder()) {
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
                                            int pieces = row.getTotalPieces()
                                            int done = row.getDonePieces()
                                            int percent = -1
                                            if ( pieces != 0 ) {
                                                percent = (done * 100) / pieces
                                            }
                                            long size = row.getTotalSize()
                                            String totalSize = ""
                                            if (size >= 0 ) {
                                                totalSize = " of " + DataHelper.formatSize2Decimal(size, false) + "B"
                                            }
                                            String.format("%02d", percent) + "% ${totalSize} ($done/$pieces pcs)".toString()
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
                                        closureColumn(header : "Count", type : String, read : {
                                            it.count.toString()
                                        })
                                        closureColumn(header : "Timestamp", type : String, read : {
                                            String.format("%02d", it.timestamp.get(Calendar.HOUR_OF_DAY)) + ":" +
                                                    String.format("%02d", it.timestamp.get(Calendar.MINUTE)) + ":" +
                                                    String.format("%02d", it.timestamp.get(Calendar.SECOND))
                                        })
                                    }
                                }
                            }
                        }
                    }
                    panel(constraints : "trust window") {
                        gridLayout(rows : 2, cols : 1)
                        panel {
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
                                panel (constraints : BorderLayout.SOUTH) {
                                    gridBagLayout()
                                    button(text : "Subscribe", enabled : bind {model.subscribeButtonEnabled}, constraints : gbc(gridx: 0, gridy : 0), subscribeAction)
                                    button(text : "Mark Neutral", enabled : bind {model.markNeutralFromTrustedButtonEnabled}, constraints : gbc(gridx: 1, gridy: 0), markNeutralFromTrustedAction)
                                    button(text : "Mark Distrusted", enabled : bind {model.markDistrustedButtonEnabled}, constraints : gbc(gridx: 2, gridy:0), markDistrustedAction)
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
                                panel(constraints : BorderLayout.SOUTH) {
                                    gridBagLayout()
                                    button(text: "Mark Neutral", enabled : bind {model.markNeutralFromDistrustedButtonEnabled}, constraints: gbc(gridx: 0, gridy: 0), markNeutralFromDistrustedAction)
                                    button(text: "Mark Trusted", enabled : bind {model.markTrustedButtonEnabled}, constraints : gbc(gridx: 1, gridy : 0), markTrustedAction)
                                }
                            }
                        }
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH){
                                label(text : "Trust List Subscriptions")
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "subscription-table", autoCreateRowSorter : true) {
                                    tableModel(list : model.subscriptions) {
                                        closureColumn(header : "Name", preferredWidth: 200, type: String, read : {it.persona.getHumanReadableName()})
                                        closureColumn(header : "Trusted", preferredWidth : 20, type: Integer, read : {it.good.size()})
                                        closureColumn(header : "Distrusted", preferredWidth: 20, type: Integer, read : {it.bad.size()})
                                        closureColumn(header : "Status", preferredWidth: 30, type: String, read : {it.status.toString()})
                                        closureColumn(header : "Last Updated", preferredWidth: 200, type : String, read : {
                                            if (it.timestamp == 0)
                                                return "Never"
                                            else
                                                return String.valueOf(new Date(it.timestamp))
                                        })
                                    }
                                }
                            }
                            panel(constraints : BorderLayout.SOUTH) {
                                button(text : "Review", enabled : bind {model.reviewButtonEnabled}, reviewAction)
                                button(text : "Update", enabled : bind {model.updateButtonEnabled}, updateAction)
                                button(text : "Unsubscribe", enabled : bind {model.unsubscribeButtonEnabled}, unsubscribeAction)
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

        def mainFrame = builder.getVariable("main-frame")
        mainFrame.setTransferHandler(new TransferHandler() {
                    public boolean canImport(TransferHandler.TransferSupport support) {
                        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                    }
                    public boolean importData(TransferHandler.TransferSupport support) {
                        def files = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor)
                        files.each {
                            model.core.eventBus.publish(new FileSharedEvent(file : it))
                        }
                        showUploadsWindow.call()
                        true
                    }
                })
        
        if (!application.getContext().get("tray-icon")) {
            mainFrame.addWindowListener(new WindowAdapter(){
                public void windowClosing(WindowEvent e) {
                    application.getWindowManager().findWindow("shutdown-window").setVisible(true)
                    Core core = application.getContext().get("core")
                    if (core != null) {
                        Thread t = new Thread({
                            core.shutdown()
                            application.shutdown()
                        }as Runnable)
                        t.start()
                    }
                }
            })
        }
            
        def downloadsTable = builder.getVariable("downloads-table")
        def selectionModel = downloadsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            def downloadDetailsPanel = builder.getVariable("download-details-panel")
            int selectedRow = selectedDownloaderRow()
            if (selectedRow < 0) {
                model.cancelButtonEnabled = false
                model.retryButtonEnabled = false
                model.pauseButtonEnabled = false
                model.downloader = null
                downloadDetailsPanel.getLayout().show(downloadDetailsPanel,"select-download")
                return
            }
            def downloader = model.downloads[selectedRow]?.downloader
            if (downloader == null)
                return
            model.downloader = downloader
            downloadDetailsPanel.getLayout().show(downloadDetailsPanel,"download-selected")
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
        downloadsTable.setDefaultRenderer(Downloader.class, new DownloadProgressRenderer())

        downloadsTable.rowSorter.addRowSorterListener({evt -> lastDownloadSortEvent = evt})
        downloadsTable.rowSorter.setSortsOnUpdates(true)
        downloadsTable.rowSorter.setComparator(2, new DownloaderComparator())

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

        // shared files menu
        JPopupMenu sharedFilesMenu = new JPopupMenu()
        JMenuItem copyHashToClipboard = new JMenuItem("Copy hash to clipboard")
        copyHashToClipboard.addActionListener({mvcGroup.view.copyHashToClipboard()})
        sharedFilesMenu.add(copyHashToClipboard)
        JMenuItem unshareSelectedFiles = new JMenuItem("Unshare selected files")
        unshareSelectedFiles.addActionListener({mvcGroup.controller.unshareSelectedFile()})
        sharedFilesMenu.add(unshareSelectedFiles)
        JMenuItem commentSelectedFiles = new JMenuItem("Comment selected files")
        commentSelectedFiles.addActionListener({mvcGroup.controller.addComment()})
        sharedFilesMenu.add(commentSelectedFiles)
        
        def sharedFilesMouseListener = new MouseAdapter() {
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
                }

        // shared files table and tree
        def sharedFilesTable = builder.getVariable("shared-files-table")
        sharedFilesTable.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())

        sharedFilesTable.rowSorter.addRowSorterListener({evt -> lastSharedSortEvent = evt})
        sharedFilesTable.rowSorter.setSortsOnUpdates(true)

        sharedFilesTable.addMouseListener(sharedFilesMouseListener)

        selectionModel = sharedFilesTable.getSelectionModel()
        selectionModel.addListSelectionListener({
            def selectedFiles = selectedSharedFiles()
            if (selectedFiles == null || selectedFiles.isEmpty())
                return
            model.addCommentButtonEnabled = true
        })
        def sharedFilesTree = builder.getVariable("shared-files-tree")
        sharedFilesTree.addMouseListener(sharedFilesMouseListener)

        sharedFilesTree.addTreeSelectionListener({
            def selectedNode = sharedFilesTree.getLastSelectedPathComponent()
            model.addCommentButtonEnabled = selectedNode != null

        })

        // searches table
        def searchesTable = builder.getVariable("searches-table")
        JPopupMenu searchTableMenu = new JPopupMenu()

        JMenuItem copySearchToClipboard = new JMenuItem("Copy search to clipboard")
        copySearchToClipboard.addActionListener({mvcGroup.view.copySearchToClipboard(searchesTable)})
        JMenuItem trustSearcher = new JMenuItem("Trust searcher")
        trustSearcher.addActionListener({mvcGroup.controller.trustPersonaFromSearch()})
        JMenuItem distrustSearcher = new JMenuItem("Distrust searcher")
        distrustSearcher.addActionListener({mvcGroup.controller.distrustPersonaFromSearch()})

        searchTableMenu.add(copySearchToClipboard)
        searchTableMenu.add(trustSearcher)
        searchTableMenu.add(distrustSearcher)

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

        // subscription table
        def subscriptionTable = builder.getVariable("subscription-table")
        subscriptionTable.setDefaultRenderer(Integer.class, centerRenderer)
        subscriptionTable.rowSorter.addRowSorterListener({evt -> trustTablesSortEvents["subscription-table"] = evt})
        subscriptionTable.rowSorter.setSortsOnUpdates(true)
        selectionModel = subscriptionTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = getSelectedTrustTablesRow("subscription-table")
            if (selectedRow < 0) {
                model.reviewButtonEnabled = false
                model.updateButtonEnabled = false
                model.unsubscribeButtonEnabled = false
                return
            }
            def trustList = model.subscriptions[selectedRow]
            if (trustList == null)
                return
            switch(trustList.status) {
                case RemoteTrustList.Status.NEW:
                case RemoteTrustList.Status.UPDATING:
                    model.reviewButtonEnabled = false
                    model.updateButtonEnabled = false
                    model.unsubscribeButtonEnabled = false
                    break
                case RemoteTrustList.Status.UPDATED:
                    model.reviewButtonEnabled = true
                    model.updateButtonEnabled = true
                    model.unsubscribeButtonEnabled = true
                    break
                case RemoteTrustList.Status.UPDATE_FAILED:
                    model.reviewButtonEnabled = false
                    model.updateButtonEnabled = true
                    model.unsubscribeButtonEnabled = true
                    break
            }
        })

        // trusted table
        def trustedTable = builder.getVariable("trusted-table")
        trustedTable.rowSorter.addRowSorterListener({evt -> trustTablesSortEvents["trusted-table"] = evt})
        trustedTable.rowSorter.setSortsOnUpdates(true)
        selectionModel = trustedTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = getSelectedTrustTablesRow("trusted-table")
            if (selectedRow < 0) {
                model.subscribeButtonEnabled = false
                model.markDistrustedButtonEnabled = false
                model.markNeutralFromTrustedButtonEnabled = false
            } else {
                model.subscribeButtonEnabled = true
                model.markDistrustedButtonEnabled = true
                model.markNeutralFromTrustedButtonEnabled = true
            }
        })

        // distrusted table
        def distrustedTable = builder.getVariable("distrusted-table")
        distrustedTable.rowSorter.addRowSorterListener({evt -> trustTablesSortEvents["distrusted-table"] = evt})
        distrustedTable.rowSorter.setSortsOnUpdates(true)
        selectionModel = distrustedTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = getSelectedTrustTablesRow("distrusted-table")
            if (selectedRow < 0) {
                model.markTrustedButtonEnabled = false
                model.markNeutralFromDistrustedButtonEnabled = false
            } else {
                model.markTrustedButtonEnabled = true
                model.markNeutralFromDistrustedButtonEnabled = true
            }
        })
        
        // show tree by default
        showSharedFilesTree.call()
    }

    private static void showPopupMenu(JPopupMenu menu, MouseEvent event) {
        menu.show(event.getComponent(), event.getX(), event.getY())
    }

    def selectedSharedFiles() {
        if (!model.treeVisible) {
            def sharedFilesTable = builder.getVariable("shared-files-table")
            int[] selected = sharedFilesTable.getSelectedRows()
            if (selected.length == 0)
                return null
            List<SharedFile> rv = new ArrayList<>()
            if (lastSharedSortEvent != null) {
                for (int i = 0; i < selected.length; i ++) {
                    selected[i] = sharedFilesTable.rowSorter.convertRowIndexToModel(selected[i])
                }
            }
            selected.each {
                rv.add(model.shared[it])
            }
            return rv
        } else {
            def sharedFilesTree = builder.getVariable("shared-files-tree")
            List<SharedFile> rv = new ArrayList<>()
            for (TreePath path : sharedFilesTree.getSelectionPaths()) {
                getLeafs(path.getLastPathComponent(), rv)
            }
            return rv
        }
    }
    
    private static void getLeafs(TreeNode node, List<SharedFile> dest) {
        if (node.isLeaf()) {
            dest.add(node.getUserObject())
            return
        }
        def children = node.children()
        while(children.hasMoreElements()) {
            getLeafs(children.nextElement(), dest)
        }
    }
    
    def copyHashToClipboard() {
        def selectedFiles = selectedSharedFiles()
        if (selectedFiles == null)
            return
        String roots = ""
        for (Iterator<SharedFile> iterator = selectedFiles.iterator(); iterator.hasNext(); ) {
            SharedFile selected = iterator.next()
            String root = Base64.encode(selected.infoHash.getRoot())
            roots += root
            if (iterator.hasNext())
                roots += "\n"
        }
        StringSelection selection = new StringSelection(roots)
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
        def downloadsTable = builder.getVariable("downloads-table")
        int selected = downloadsTable.getSelectedRow()
        if (selected < 0)
            return selected
        if (lastDownloadSortEvent != null)
            selected = downloadsTable.rowSorter.convertRowIndexToModel(selected)
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
        model.searchesPaneButtonEnabled = false
        model.downloadsPaneButtonEnabled = true
        model.uploadsPaneButtonEnabled = true
        model.monitorPaneButtonEnabled = true
        model.trustPaneButtonEnabled = true
    }

    def showDownloadsWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "downloads window")
        model.searchesPaneButtonEnabled = true
        model.downloadsPaneButtonEnabled = false
        model.uploadsPaneButtonEnabled = true
        model.monitorPaneButtonEnabled = true
        model.trustPaneButtonEnabled = true
    }

    def showUploadsWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "uploads window")
        model.searchesPaneButtonEnabled = true
        model.downloadsPaneButtonEnabled = true
        model.uploadsPaneButtonEnabled = false
        model.monitorPaneButtonEnabled = true
        model.trustPaneButtonEnabled = true
    }

    def showMonitorWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel,"monitor window")
        model.searchesPaneButtonEnabled = true
        model.downloadsPaneButtonEnabled = true
        model.uploadsPaneButtonEnabled = true
        model.monitorPaneButtonEnabled = false
        model.trustPaneButtonEnabled = true
    }

    def showTrustWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel,"trust window")
        model.searchesPaneButtonEnabled = true
        model.downloadsPaneButtonEnabled = true
        model.uploadsPaneButtonEnabled = true
        model.monitorPaneButtonEnabled = true
        model.trustPaneButtonEnabled = false
    }
    
    def showSharedFilesTable = {
        model.treeVisible = false
        def cardsPanel = builder.getVariable("shared-files-panel")
        cardsPanel.getLayout().show(cardsPanel, "shared files table")
    }
    
    def showSharedFilesTree = {
        model.treeVisible = true
        def cardsPanel = builder.getVariable("shared-files-panel")
        cardsPanel.getLayout().show(cardsPanel, "shared files tree")
    }

    def shareFiles = {
        def chooser = new JFileChooser()
        chooser.setFileHidingEnabled(!model.core.muOptions.shareHiddenFiles)
        chooser.setDialogTitle("Select file to share")
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY)
        chooser.setMultiSelectionEnabled(true)
        int rv = chooser.showOpenDialog(null)
        if (rv == JFileChooser.APPROVE_OPTION) {
            chooser.getSelectedFiles().each {
                File canonical = it.getCanonicalFile()
                model.core.eventBus.publish(new FileSharedEvent(file : canonical))
            }
        }
    }

    int getSelectedTrustTablesRow(String tableName) {
        def table = builder.getVariable(tableName)
        int selectedRow = table.getSelectedRow()
        if (selectedRow < 0)
            return -1
        if (trustTablesSortEvents.get(tableName) != null)
            selectedRow = table.rowSorter.convertRowIndexToModel(selectedRow)
        selectedRow
    }
    
    public void refreshSharedFiles() {
        def tree = builder.getVariable("shared-files-tree")
        TreePath[] selectedPaths = tree.getSelectionPaths()
        model.sharedTree.nodeStructureChanged(model.treeRoot)
        tree.setSelectionPaths(selectedPaths)
        builder.getVariable("shared-files-table").model.fireTableDataChanged()
    }
}