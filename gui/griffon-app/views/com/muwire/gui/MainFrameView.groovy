package com.muwire.gui

import com.muwire.core.messenger.MWMessage
import com.muwire.core.messenger.Messenger
import com.muwire.core.messenger.UIMessageMovedEvent
import com.muwire.core.trust.TrustLevel
import griffon.core.GriffonApplication
import griffon.core.mvc.MVCGroup

import javax.swing.DropMode
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTextField
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.tree.DefaultMutableTreeNode
import java.awt.GridBagConstraints

import static com.muwire.gui.Translator.trans
import griffon.core.artifact.GriffonView
import griffon.core.env.Metadata
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64
import net.i2p.data.DataHelper

import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.ListSelectionModel
import javax.swing.TransferHandler
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.SharedFile
import com.muwire.core.collections.FileCollection
import com.muwire.core.download.Downloader
import com.muwire.core.filefeeds.Feed
import com.muwire.core.filefeeds.FeedItem
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.trust.RemoteTrustList
import com.muwire.core.upload.Uploader

import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.annotation.Nonnull
import javax.inject.Inject

@ArtifactProviderFor(GriffonView)
class MainFrameView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    MainFrameModel model
    @MVCMember @Nonnull
    MainFrameController controller

    @Inject @Nonnull GriffonApplication application
    @Inject Metadata metadata

    def downloadsTable
    def lastDownloadSortEvent
    def lastUploadsSortEvent
    def lastSharedSortEvent
    def lastContactsSortEvent
    def lastContactsSubscriptionSortEvent
    def expansionListener = new TreeExpansions()
    ResultTabsChangeListener resultsTabListener
    def lastFeedsSortEvent
    def lastFeedItemsSortEvent
    

    UISettings settings
    ChatNotificator chatNotificator
    
    JTable collectionsTable
    def lastCollectionSortEvent
    JTable collectionFilesTable
    def lastCollectionFilesSortEvent
    
    JList systemMessageFolderList, userMessageFolderList
    JPanel messageFolderContents
    
    void initUI() {
        chatNotificator = new ChatNotificator(application.getMvcGroupManager())
        settings = application.context.get("ui-settings")
        int rowHeight = application.context.get("row-height")
        
        int mainFrameX = 1
        int mainFrameY = 1
        int dividerLocation = 750
        boolean pack = true
        if (settings.mainFrameX > 0 && settings.mainFrameY > 0) {
            pack = false
            mainFrameX = settings.mainFrameX
            mainFrameY = settings.mainFrameY
            dividerLocation = (int)(mainFrameY * 0.75d)
        }
        
        def transferHandler = new MWTransferHandler()
        def collectionsTransferHandler = new FileCollectionTransferHandler()
            
        builder.with {
            application(size : [mainFrameX,mainFrameY], id: 'main-frame',
            locationRelativeTo : null,
            defaultCloseOperation : JFrame.DO_NOTHING_ON_CLOSE,
            title: application.configuration['application.title'] + " " +
            metadata["application.version"],
            iconImage:   imageIcon('/MuWire-48x48.png').image,
            iconImages: [imageIcon('/MuWire-48x48.png').image,
                imageIcon('/MuWire-32x32.png').image,
                imageIcon('/MuWire-16x16.png').image],
            pack : pack,
            visible : bind { model.coreInitialized }) {
                menuBar {
                    menu (text : trans("FILE")) {
                        menuItem(trans("EXIT"), actionPerformed : {closeApplication()})
                    }
                    menu (text : trans("OPTIONS")) {
                        menuItem(trans("CONFIGURATION"), actionPerformed : {
                            def params = [:]
                            params['core'] = application.context.get("core")
                            params['settings'] = params['core'].muOptions
                            params['uiSettings'] = settings 
                            mvcGroup.createMVCGroup("Options", params)
                        })
                    }
                    menu (text : trans("STATUS")) {
                        menuItem("MuWire", actionPerformed : {mvcGroup.createMVCGroup("mu-wire-status")})
                        MuWireSettings muSettings = application.context.get("muwire-settings")
                        menuItem("I2P", enabled : bind {model.routerPresent}, actionPerformed: {mvcGroup.createMVCGroup("i-2-p-status")})
                        menuItem(trans("SYSTEM"), actionPerformed : {mvcGroup.createMVCGroup("system-status")})
                    }
                    menu (text : trans("TOOLS")) {
                        menuItem(trans("CONTENT_CONTROL"), actionPerformed : {
                            def env = [:]
                            env["core"] = model.core
                            mvcGroup.createMVCGroup("content-panel", env)
                        })
                        menuItem(trans("ADVANCED_SHARING"), actionPerformed : {
                            def env = [:]
                            env["core"] = model.core
                            mvcGroup.createMVCGroup("advanced-sharing",env)  
                        })
                        menuItem(trans("CERTIFICATES"), actionPerformed : {
                            def env = [:]
                            env['core'] = model.core
                            mvcGroup.createMVCGroup("certificate-control",env)
                        })
                        menuItem(trans("CHAT_ROOM_MONITOR"), actionPerformed : {
                            if (!mvcGroup.getChildrenGroups().containsKey("chat-monitor")) {
                                def env = [:]
                                env['chatNotificator'] = chatNotificator
                                mvcGroup.createMVCGroup("chat-monitor","chat-monitor",env)
                            }
                        })
                        menuItem(trans("SIGN_TOOL"), actionPerformed : {
                            def env = [:]
                            env['core'] = model.core
                            mvcGroup.createMVCGroup("sign",env)
                        })
                    }
                }
                borderLayout()
                panel (border: etchedBorder(), constraints : BorderLayout.NORTH) {
                    borderLayout()
                    panel (constraints: BorderLayout.WEST) {
                        gridLayout(rows:1, cols: 2)
                        button(text: trans("SEARCHES"), enabled : bind{model.searchesPaneButtonEnabled},actionPerformed : showSearchWindow)
                        button(text: trans("DOWNLOADS"), enabled : bind{model.downloadsPaneButtonEnabled}, actionPerformed : showDownloadsWindow)
                        button(text: trans("LIBRARY"), enabled : bind{model.uploadsPaneButtonEnabled}, actionPerformed : showUploadsWindow)
                        button(text: trans("COLLECTIONS"), enabled : bind{model.collectionsPaneButtonEnabled}, actionPerformed : showCollectionsWindow)
                        if (settings.showMonitor)
                            button(text: trans("MONITOR"), enabled: bind{model.monitorPaneButtonEnabled},actionPerformed : showMonitorWindow)
                        button(text: trans("FEEDS"), enabled: bind {model.feedsPaneButtonEnabled}, actionPerformed : showFeedsWindow)
                        button(text : trans("MESSAGES"), enabled: bind {model.messagesPaneButtonEnabled},actionPerformed : showMessagesWindow)
                        button(text: trans("CHAT"), enabled : bind{model.chatPaneButtonEnabled}, actionPerformed : showChatWindow)
                        button(text: trans("CONTACTS"), enabled:bind{model.trustPaneButtonEnabled},actionPerformed : showTrustWindow)
                    }
                    panel(id: "top-panel", constraints: BorderLayout.CENTER) {
                        cardLayout()
                        label(constraints : "top-connect-panel",
                        text : "        " + trans("MUWIRE_IS_CONNECTING")) // TODO: real padding
                        panel(constraints : "top-search-panel") {
                            borderLayout()
                            panel(constraints: BorderLayout.CENTER) {
                                borderLayout()
                                label("        " + trans("ENTER_SEARCH")+ " ", constraints: BorderLayout.WEST) // TODO: fix this
                                
                                def searchFieldModel = new SearchFieldModel(settings, new File(application.context.get("muwire-home")))
                                JComboBox myComboBox = new SearchField(searchFieldModel)
                                myComboBox.setAction(searchAction)
                                widget(id: "search-field", constraints: BorderLayout.CENTER, myComboBox)

                            }
                            panel( constraints: BorderLayout.EAST) {
                                button(text: trans("SEARCH"), searchAction)
                            }
                        }
                    }
                }
                panel (id: "cards-panel", constraints : BorderLayout.CENTER) {
                    cardLayout()
                    panel (id : "search window", constraints : "search window") {
                        cardLayout()
                        panel (constraints : "tabs-panel") {
                            borderLayout()
                            tabbedPane(id : "result-tabs", constraints: BorderLayout.CENTER)
                        }
                        panel(constraints : "restore session") {
                            borderLayout()
                            panel (constraints : BorderLayout.CENTER) {
                                gridBagLayout()
                                label(text :  trans("SAVED_TABS") + ":", constraints : gbc(gridx : 0, gridy : 0))
                                scrollPane (constraints : gbc(gridx : 0, gridy : 1)) {
                                    list(items : new ArrayList(settings.openTabs))
                                }
                                button(text : trans("RESTORE_SESSION"), constraints : gbc(gridx :0, gridy : 2), restoreSessionAction)
                            }
                        } 
                    }
                    panel (constraints: "downloads window") {
                        gridLayout(rows : 1, cols : 1)
                        splitPane(orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout : true, dividerLocation: dividerLocation) {
                            panel {
                                borderLayout()
                                scrollPane (constraints : BorderLayout.CENTER) {
                                    downloadsTable = table(id : "downloads-table", autoCreateRowSorter : true, rowHeight : rowHeight) {
                                        tableModel(list: model.downloads) {
                                            closureColumn(header: trans("NAME"), preferredWidth: 300, type: String, read : {row -> HTMLSanitizer.sanitize(row.downloader.file.getName())})
                                            closureColumn(header: trans("STATUS"), preferredWidth: 50, type: String, read : {row -> trans(row.downloader.getCurrentState().name())})
                                            closureColumn(header: trans("PROGRESS"), preferredWidth: 70, type: Downloader, read: { row -> row.downloader })
                                            closureColumn(header: trans("SPEED"), preferredWidth: 50, type:String, read :{row ->
                                                formatSize(row.downloader.speed(),"B_SEC")
                                            })
                                            closureColumn(header : trans("ETA"), preferredWidth : 50, type:String, read :{ row ->
                                                def speed = row.downloader.speed()
                                                if (speed == 0)
                                                    return trans("UNKNOWN")
                                                else {
                                                    def remaining = (row.downloader.getNPieces() - row.downloader.donePieces()) * row.downloader.getPieceSize() / speed
                                                    return DataHelper.formatDuration(remaining.toLong() * 1000)
                                                }
                                            })
                                        }
                                    }
                                }
                                panel (constraints : BorderLayout.SOUTH) {
                                    button(text: trans("PAUSE"), enabled : bind {model.pauseButtonEnabled}, pauseAction)
                                    button(text: bind { trans(model.resumeButtonText) }, enabled : bind {model.retryButtonEnabled}, resumeAction)
                                    button(text: trans("CANCEL"), enabled : bind {model.cancelButtonEnabled }, cancelAction)
                                    button(text: trans("PREVIEW"), enabled : bind {model.previewButtonEnabled}, previewAction)
                                    button(text: trans("CLEAR_DONE"), enabled : bind {model.clearButtonEnabled}, clearAction)
                                }
                            }
                            panel {
                                borderLayout()
                                panel(constraints : BorderLayout.NORTH) {
                                    label(text : trans("DOWNLOAD_DETAILS"))
                                }
                                scrollPane(constraints : BorderLayout.CENTER) {
                                    panel (id : "download-details-panel") {
                                        cardLayout()
                                        panel (constraints : "select-download") {
                                            label(text : trans("SELECT_DOWNLOAD_VIEW_DETAILS"))
                                        }
                                        panel(constraints : "download-selected") {
                                            gridBagLayout()
                                            label(text : trans("DOWNLOAD_LOCATION") + ":", constraints : gbc(gridx:0, gridy:0))
                                            label(text : bind { HTMLSanitizer.sanitize(model.downloader?.file?.getAbsolutePath()) },
                                            constraints: gbc(gridx:1, gridy:0, gridwidth: 2, insets : [0,0,0,20]))
                                            label(text : trans("PIECE_SIZE") + ":", constraints : gbc(gridx: 0, gridy:1))
                                            label(text : bind {model.downloader?.pieceSize}, constraints : gbc(gridx:1, gridy:1))
                                            label(text : trans("SEQUENTIAL") + ":", constraints : gbc(gridx: 0, gridy: 2))
                                            label(text : bind {model.downloader?.isSequential()}, constraints : gbc(gridx:1, gridy:2, insets : [0,0,0,20]))
                                            label(text : trans("KNOWN_SOURCES") + ":", constraints : gbc(gridx:3, gridy: 0))
                                            label(text : bind {model.downloader?.activeWorkers()}, constraints : gbc(gridx:4, gridy:0, insets : [0,0,0,20]))
                                            label(text : trans("ACTIVE_SOURCES") + ":", constraints : gbc(gridx:3, gridy:1))
                                            label(text : bind {model.downloader?.activeWorkers()}, constraints : gbc(gridx:4, gridy:1, insets : [0,0,0,20]))
                                            label(text : trans("HOPELESS_SOURCES") + ":", constraints : gbc(gridx:3, gridy:2))
                                            label(text : bind {model.downloader?.countHopelessSources()}, constraints : gbc(gridx:4, gridy:2, insets : [0,0,0,20]))
                                            label(text : trans("TOTAL_PIECES") + ":", constraints : gbc(gridx:5, gridy: 0))
                                            label(text : bind {model.downloader?.getNPieces()}, constraints : gbc(gridx:6, gridy:0, insets : [0,0,0,20]))
                                            label(text : trans("DONE_PIECES") + ":", constraints: gbc(gridx:5, gridy: 1))
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
                                    if (model.hashingFile == null && model.hashingFiles == 0) {
                                        trans("YOU_CAN_DRAG_AND_DROP")
                                    } else if (model.hashingFiles == 1 && model.hashingFile != null) {
                                        trans("HASHING") + ": " +
                                            model.hashingFile.getAbsolutePath() + " (" + formatSize(model.hashingFile.length(),"BYTES_SHORT") + ")"
                                    } else {
                                        trans("HASHING") + " " + model.hashingFiles + " " + trans("FILES")
                                    }
                                })
                            }
                            panel (border : etchedBorder(), constraints : BorderLayout.CENTER) {
                                borderLayout()
                                panel (id : "shared-files-panel", constraints : BorderLayout.CENTER){
                                    cardLayout()
                                    panel (constraints : "shared files table", transferHandler: transferHandler) {
                                        borderLayout()
                                        scrollPane(constraints : BorderLayout.CENTER) {
                                            JTable table = table(id : "shared-files-table", autoCreateRowSorter: true, rowHeight : rowHeight, 
                                                dragEnabled : true, transferHandler: transferHandler) {
                                                tableModel(list : model.shared) {
                                                    closureColumn(header : trans("NAME"), preferredWidth : 500, type : String, read : {row -> row.getCachedPath()})
                                                    closureColumn(header : trans("SIZE"), preferredWidth : 50, type : Long, read : {row -> row.getCachedLength() })
                                                    closureColumn(header : trans("COMMENTS"), preferredWidth : 50, type : Boolean, read : {it.getComment() != null})
                                                    closureColumn(header : trans("CERTIFIED"), preferredWidth : 50, type : Boolean, read : {
                                                        Core core = application.context.get("core")
                                                        core.certificateManager.hasLocalCertificate(it.getRootInfoHash())
                                                    })
                                                    closureColumn(header : trans("PUBLISHED"), preferredWidth : 50, type : Boolean, read : {row -> row.isPublished()})
                                                    closureColumn(header : trans("SEARCH_HITS"), preferredWidth: 50, type : Integer, read : {it.getHits()})
                                                    closureColumn(header : trans("DOWNLOADERS"), preferredWidth: 50, type : Integer, read : {it.getDownloaders().size()})
                                                }
                                            }
                                        }
                                    }
                                    panel (constraints : "shared files tree") {
                                        borderLayout()
                                        scrollPane(constraints : BorderLayout.CENTER) {
                                            def jtree = new JTree(model.sharedTree)
                                            jtree.setRowHeight(rowHeight)
                                            jtree.setCellRenderer(new SharedTreeRenderer())
                                            jtree.setDragEnabled(true)
                                            jtree.setTransferHandler(transferHandler)
                                            tree(id : "shared-files-tree", rowHeight : rowHeight, rootVisible : false, expandsSelectedPaths: true, largeModel : true, jtree)
                                        }
                                    }
                                }
                            }
                            panel (constraints : BorderLayout.SOUTH) {
                                gridLayout(rows:1, cols:4)
                                panel {
                                    buttonGroup(id : "sharedViewType")
                                    radioButton(text : trans("TREE"), selected : true, buttonGroup : sharedViewType, actionPerformed : showSharedFilesTree)
                                    radioButton(text : trans("TABLE"), selected : false, buttonGroup : sharedViewType, actionPerformed : showSharedFilesTable)
                                }
                                panel {
                                    gridBagLayout()
                                    button(text : trans("ADD_COMMENT"), enabled : bind {model.addCommentButtonEnabled}, constraints : gbc(gridx: 0), addCommentAction)
                                    button(text : trans("CERTIFY"), enabled : bind {model.addCommentButtonEnabled}, constraints : gbc(gridx: 1), issueCertificateAction)
                                    button(text : bind {trans(model.publishButtonText)}, enabled : bind {model.publishButtonEnabled}, constraints : gbc(gridx:2), publishAction)
                                }
                                panel {
                                    def textField = new JTextField(columns: 10)
                                    textField.addActionListener({controller.filterLibrary()})
                                    widget(id: "library-filter-textfield", enabled: bind{model.filteringEnabled}, textField)
                                    button(text: trans("FILTER"), enabled : bind {model.filteringEnabled}, filterLibraryAction)
                                    button(text: trans("CLEAR"), enabled : bind{model.filteringEnabled}, clearLibraryFilterAction)
                                }
                                panel {
                                    panel {
                                        label(trans("SHARED") + ":")
                                        label(text : bind {model.loadedFiles}, id : "shared-files-count")
                                    }
                                    button(text : trans("SHARE"), actionPerformed : shareFiles)
                                }
                            }
                        }
                        panel (border : etchedBorder()) {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH){
                                label(trans("UPLOADS"))
                            }
                            scrollPane (constraints : BorderLayout.CENTER) {
                                table(id : "uploads-table", autoCreateRowSorter: true, rowHeight : rowHeight) {
                                    tableModel(list : model.uploads) {
                                        closureColumn(header : trans("NAME"), preferredWidth: 350,  type : String, read : {row -> HTMLSanitizer.sanitize(row.uploader.getName()) })
                                        closureColumn(header : trans("PROGRESS"), preferredWidth: 50, type : String, read : { row ->
                                            int percent = row.uploader.getProgress()
                                            trans("PERCENT_OF_PIECE", percent)
                                        })
                                        closureColumn(header : trans("DOWNLOADER"), type : String, read : { row ->
                                            row.uploader.getDownloader()
                                        })
                                        closureColumn(header : trans("REMOTE_PIECES"), type : String, read : { row ->
                                            int pieces = row.uploader.getTotalPieces()
                                            int done = row.uploader.getDonePieces()
                                            int percent = -1
                                            if ( pieces != 0 ) {
                                                percent = (done * 100) / pieces
                                            }
                                            long size = row.uploader.getTotalSize()
                                            String totalSize = ""
                                            if (size >= 0 ) {
                                                totalSize = trans("PERCENT_OF",
                                                        String.format("%02d", percent),
                                                        formatSize(size, "BYTES_SHORT"))
                                            }
                                            "${totalSize} ($done/$pieces".toString() + trans("PIECES_SHORT")+ ")"
                                        })
                                        closureColumn(header : trans("SPEED"), preferredWidth: 50,  type : String, read : { row ->
                                            int speed = row.speed()
                                            formatSize(speed, "B_SEC")
                                        })
                                    }
                                }
                            }
                            panel (constraints : BorderLayout.SOUTH) {
                                button(text : trans("CLEAR_FINISHED_UPLOADS"), clearUploadsAction)
                            }
                        }
                    }
                    panel (constraints : "collections window") {
                        gridLayout(rows: 2, cols : 1)
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH) {
                                label(text : trans("COLLECTION_TOOL_HEADER"))
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "collections-table", autoCreateRowSorter : true, rowHeight : rowHeight,
                                    dragEnabled : true, transferHandler : collectionsTransferHandler) {
                                    tableModel(list : model.localCollections) {
                                        closureColumn(header : trans("NAME"), preferredWidth : 100, type : String, read : {HTMLSanitizer.sanitize(it.name)})
                                        closureColumn(header : trans("AUTHOR"), preferredWidth : 100, type : String, read : {it.author.getHumanReadableName()})
                                        closureColumn(header : trans("FILES"), preferredWidth: 10, type : Integer, read : {it.numFiles()})
                                        closureColumn(header : trans("SIZE"), preferredWidth : 10, type : Long, read : {it.totalSize()})
                                        closureColumn(header : trans("COMMENT"), preferredWidth : 10, type : Boolean, read : {it.comment != ""})
                                        closureColumn(header : trans("SEARCH_HITS"), preferredWidth : 10, type : Integer, read : {it.hits.size()})
                                        closureColumn(header : trans("CREATED"), preferredWidth : 30, type : Long, read : {it.timestamp})
                                    }
                                }
                            }
                            panel(constraints : BorderLayout.SOUTH) {
                                gridLayout(rows : 1, cols : 2)
                                panel {
                                    button(text : trans("CREATE_COLLECTION"), collectionAction)
                                    button(text : trans("DELETE"), enabled : bind {model.deleteCollectionButtonEnabled}, deleteCollectionAction)
                                }
                                panel {
                                    button(text : trans("VIEW_COMMENT"), enabled : bind {model.viewCollectionCommentButtonEnabled}, viewCollectionCommentAction)
                                    button(text : trans("COLLECTION_SHOW_HITS"), enabled : bind {model.deleteCollectionButtonEnabled}, showCollectionToolAction)
                                    button(text : trans("COPY_HASH_TO_CLIPBOARD"), enabled : bind {model.deleteCollectionButtonEnabled}, copyCollectionHashAction)
                                }
                            }
                        }
                        panel {
                            borderLayout()
                            panel(constraints : BorderLayout.NORTH) {
                                label(text : trans("FILES"))
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "items-table", autoCreateRowSorter : true, rowHeight : rowHeight) {
                                    tableModel(list : model.collectionFiles) {
                                        closureColumn(header : trans("NAME"), preferredWidth : 200, type : String, read : {HTMLSanitizer.sanitize(it.getCachedPath())})
                                        closureColumn(header : trans("SIZE"), preferredWidth : 10, type : Long, read : {it.getCachedLength()})
                                        closureColumn(header : trans("COMMENT"), preferredWidth : 10, type : Boolean, read : {it.getComment() != null})
                                    }
                                }
                            }
                            panel(constraints : BorderLayout.SOUTH) {
                                button(text : trans("VIEW_COMMENT"), enabled : bind{model.viewItemCommentButtonEnabled}, viewItemCommentAction)
                            }
                        }
                    }
                    panel (constraints: "monitor window") {
                        gridLayout(rows : 1, cols : 2)
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH){
                                label(trans("CONNECTIONS"))
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "connections-table", rowHeight : rowHeight) {
                                    tableModel(list : model.connectionList) {
                                        closureColumn(header : trans("DESTINATION"), preferredWidth: 250, type: String, read : { row -> row.destination.toBase32() })
                                        closureColumn(header : trans("DIRECTION"), preferredWidth: 20, type: String, read : { row ->
                                            if (row.incoming)
                                                return trans("IN")
                                            else
                                                return trans("OUT")
                                        })
                                    }
                                }
                            }
                        }
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH){
                                label(trans("INCOMING_SEARCHES"))
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "searches-table", rowHeight : rowHeight) {
                                    tableModel(list : model.searches) {
                                        closureColumn(header : trans("KEYWORDS"), type : String, read : {
                                            HTMLSanitizer.sanitize(it.search)
                                        })
                                        closureColumn(header : trans("FROM"), type : String, read : {
                                            if (it.originator != null) {
                                                return it.originator.getHumanReadableName()
                                            } else {
                                                return it.replyTo.toBase32()
                                            }
                                        })
                                        closureColumn(header : trans("COUNT"), type : String, read : {
                                            it.count.toString()
                                        })
                                        closureColumn(header : trans("TIMESTAMP"), type : String, read : {
                                            String.format("%02d", it.timestamp.get(Calendar.HOUR_OF_DAY)) + ":" +
                                                    String.format("%02d", it.timestamp.get(Calendar.MINUTE)) + ":" +
                                                    String.format("%02d", it.timestamp.get(Calendar.SECOND))
                                        })
                                    }
                                }
                            }
                        }
                    }
                    panel(constraints : "feeds window") {
                        gridLayout(rows : 2, cols : 1)
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH) {
                                label(text: trans("SUBSCRIPTIONS"))
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "feeds-table", autoCreateRowSorter : true, rowHeight : rowHeight) {
                                    tableModel(list : model.feeds) {
                                        closureColumn(header : trans("PUBLISHER"), preferredWidth: 350, type : String, read : {it.getPublisher().getHumanReadableName()})
                                        closureColumn(header : trans("FILES"), preferredWidth: 10, type : Integer, read : {model.core.feedManager.getFeedItems(it.getPublisher()).size()})
                                        closureColumn(header : trans("LAST_UPDATED"), type : Long, read : {it.getLastUpdated()})
                                        closureColumn(header : trans("STATUS"), preferredWidth: 10, type : String, read : {trans(it.getStatus().name())})
                                    }
                                }
                            }
                            panel (constraints : BorderLayout.SOUTH) {
                                gridLayout(rows: 1, cols: 3)
                                panel(border: etchedBorder()) {
                                    button(text: trans("SUBSCRIBE"), subscribeToFeedAction)
                                    button(text : trans("UNSUBSCRIBE"), enabled : bind {model.unsubscribeFileFeedButtonEnabled}, unsubscribeFileFeedAction)
                                }
                                panel(border: etchedBorder()) {
                                    button(text: trans("UPDATE"), enabled: bind { model.updateFileFeedButtonEnabled }, updateFileFeedAction)
                                    button(text: trans("CONFIGURE"), enabled: bind { model.configureFileFeedButtonEnabled }, configureFileFeedAction)
                                }
                                panel(border: etchedBorder()) {
                                    button(text: trans("MY_FEED"), showMyFeedAction)
                                }
                            }
                        }
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH) {
                                label(text : trans("PUBLISHED_FILES"))
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "feed-items-table", autoCreateRowSorter : true, rowHeight : rowHeight) {
                                    tableModel(list : model.feedItems) {
                                        closureColumn(header : trans("NAME"), preferredWidth: 350, type : String, read : {HTMLSanitizer.sanitize(it.getName())})
                                        closureColumn(header : trans("SIZE"), preferredWidth: 10, type : Long, read : {it.getSize()})
                                        closureColumn(header : trans("COMMENT"), preferredWidth: 10, type : Boolean, read : {it.getComment() != null})
                                        closureColumn(header : trans("CERTIFICATES"), preferredWidth: 10, type : Integer, read : {it.getCertificates()})
                                        closureColumn(header : trans("DOWNLOADED"), preferredWidth: 10, type : Boolean, read : {
                                            InfoHash ih = it.getInfoHash()
                                            model.core.fileManager.isShared(ih)
                                        })
                                        closureColumn(header: trans("DATE"), type : Long, read : {it.getTimestamp()})
                                    }
                                }
                            }
                            panel(constraints : BorderLayout.SOUTH) {
                                button(text : trans("DOWNLOAD"), enabled : bind {model.downloadFeedItemButtonEnabled}, downloadFeedItemAction)
                                button(text : trans("VIEW_COMMENT"), enabled : bind {model.viewFeedItemCommentButtonEnabled}, viewFeedItemCommentAction)
                                button(text : trans("VIEW_CERTIFICATES"), enabled : bind {model.viewFeedItemCertificatesButtonEnabled}, viewFeedItemCertificatesAction )
                            }
                        }
                    }
                    panel(constraints : "trust window") {
                        gridLayout(rows : 2, cols : 1)
                        panel {
                            gridLayout(rows: 1, cols: 1)
                            panel(border: etchedBorder()) {
                                borderLayout()
                                scrollPane(constraints: BorderLayout.CENTER) {
                                    table(id: "contacts-table", autoCreateRowSorter: true, rowHeight: rowHeight,
                                            dragEnabled: true, transferHandler: new PersonaTransferHandler()) {
                                        tableModel(list: model.contacts) {
                                            closureColumn(header: trans("CONTACTS"), preferredWidth: 250, type: String, read: { it.persona.getHumanReadableName() })
                                            closureColumn(header: trans("REASON"), preferredWidth: 450, type: String, read: { it.reason })
                                            closureColumn(header: trans("TRUST_STATUS"), preferredWidth: 60, type: String, read: { row ->
                                                trans(model.core.trustService.getLevel(row.persona.destination).name())
                                            })
                                        }
                                    }
                                }
                                panel(constraints : BorderLayout.SOUTH) {
                                    gridLayout(rows : 1, cols : 2)
                                    panel (border : etchedBorder()) {
                                        button(text : trans("ADD_CONTACT"), addContactAction)
                                        button(text : trans("REMOVE_CONTACT"), enabled : bind {model.removeContactButtonEnabled}, removeContactAction)
                                        button(text : trans("MARK_TRUSTED"), enabled : bind {model.markTrustedButtonEnabled}, markTrustedAction)
                                        button(text : trans("MARK_DISTRUSTED"), enabled : bind {model.markDistrustedButtonEnabled}, markDistrustedAction)
                                    }
                                    panel (border : etchedBorder()) {
                                        button(text : trans("BROWSE"), enabled: bind {model.browseFromTrustedButtonEnabled}, browseFromTrustedAction)
                                        button(text : trans("BROWSE_COLLECTIONS"), enabled : bind {model.browseFromTrustedButtonEnabled}, browseCollectionsFromTrustedAction)
                                        button(text : trans("SUBSCRIBE"), enabled : bind {model.subscribeButtonEnabled}, subscribeAction)
                                        button(text : trans("MESSAGE_VERB"), enabled : bind {model.messageFromTrustedButtonEnabled}, messageFromTrustedAction)
                                        button(text : trans("CHAT"), enabled : bind {model.chatFromTrustedButtonEnabled}, chatFromTrustedAction)
                                    }
                                }
                            }
                        }
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH){
                                label(text : trans("TRUST_LIST_SUBSCRIPTIONS"))
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "subscription-table", autoCreateRowSorter : true, rowHeight : rowHeight) {
                                    tableModel(list : model.subscriptions) {
                                        closureColumn(header : trans("NAME"), preferredWidth: 200, type: String, read : {it.persona.getHumanReadableName()})
                                        closureColumn(header : trans("TRUSTED"), preferredWidth : 20, type: Integer, read : {it.good.size()})
                                        closureColumn(header : trans("DISTRUSTED"), preferredWidth: 20, type: Integer, read : {it.bad.size()})
                                        closureColumn(header : trans("STATUS"), preferredWidth: 30, type: String, read : {trans(it.status.name())})
                                        closureColumn(header : trans("LAST_UPDATED"), preferredWidth: 200, type : Long, read : { it.timestamp })
                                    }
                                }
                            }
                            panel(constraints : BorderLayout.SOUTH) {
                                button(text : trans("REVIEW"), enabled : bind {model.reviewButtonEnabled}, reviewAction)
                                button(text : trans("UPDATE"), enabled : bind {model.updateButtonEnabled}, updateAction)
                                button(text : trans("UNSUBSCRIBE"), enabled : bind {model.unsubscribeButtonEnabled}, unsubscribeAction)
                            }
                        }
                    }
                    panel(constraints : "messages window") {
                        gridLayout(rows : 1, cols : 1) 
                        splitPane(orientation : JSplitPane.HORIZONTAL_SPLIT, continuousLayout : true, dividerLocation : 100) {
                            panel {
                                gridBagLayout()
                                panel(border: etchedBorder(), constraints : gbc(gridx:0, gridy:0, weightx: 100, weighty: 0,
                                        fill : GridBagConstraints.BOTH)) {
                                    gridBagLayout()
                                    list(id: "message-folders-list", model: model.messageFolderListModel, 
                                            constraints: gbc(weightx: 100, anchor: GridBagConstraints.LINE_START))
                                }
                                panel(border: etchedBorder(), constraints : gbc(gridx: 0, gridy:1, fill: GridBagConstraints.HORIZONTAL,
                                        weightx:0, weighty: 0)) {
                                    gridBagLayout()
                                    label(text : trans("FOLDERS"), constraints : gbc(gridx:0, gridy: 0))
                                    button(text : trans("CREATE_FOLDER"), constraints : gbc(gridx:0, gridy: 1, weightx: 100, fill:GridBagConstraints.HORIZONTAL),
                                            createMessageFolderAction)
                                    button(text : trans("DELETE_FOLDER"), enabled : bind {model.deleteMessageFolderButtonEnabled}, 
                                            constraints : gbc(gridx:0, gridy: 2, weightx: 100, fill: GridBagConstraints.HORIZONTAL), deleteMessageFolderAction)
                                }
                                panel(border: etchedBorder(), constraints : gbc(gridx:0, gridy:2, fill: GridBagConstraints.BOTH, 
                                        weightx: 100, weighty: 100)) {
                                    gridLayout(rows: 1, cols: 1)
                                    scrollPane() {
                                        list(id: "user-message-folders-list", model: model.userMessageFolderListModel)
                                    }
                                }
                            }
                            panel (id : "message-folder-contents"){
                                cardLayout()
                            }
                        }
                    }
                    panel(constraints : "chat window") {
                        borderLayout()
                        tabbedPane(id : "chat-tabs", constraints : BorderLayout.CENTER)
                        panel(constraints : BorderLayout.SOUTH) {
                            button(text : trans("START_CHAT_SERVER"), enabled : bind {!model.chatServerRunning}, startChatServerAction)
                            button(text : trans("STOP_CHAT_SERVER"), enabled : bind {model.chatServerRunning}, stopChatServerAction)
                            button(text : trans("CONNECT_TO_REMOTE_SERVER"), connectChatServerAction)
                        }
                    }
                }
                panel (border: etchedBorder(), constraints : BorderLayout.SOUTH) {
                    borderLayout()
                    panel (constraints : BorderLayout.WEST) {
                        gridBagLayout()
                        label(text : bind {model.me}, constraints : gbc(gridx:0, gridy:0))
                        button(text : trans("COPY_SHORT"), constraints : gbc(gridx:1, gridy:0), copyShortAction)
                        button(text : trans("COPY_FULL"), constraints : gbc(gridx:2, gridy:0), copyFullAction)
                    }
                    panel (constraints : BorderLayout.CENTER) {
                        gridBagLayout()
                        panel (constraints : gbc(gridx : 0, gridy : 0)){
                            borderLayout()
                            label(icon : imageIcon('/down_arrow.png'), constraints : BorderLayout.CENTER)
                            label(text : bind { formatSize(model.downSpeed, "B_SEC") }, constraints : BorderLayout.EAST)
                        }
                        panel (constraints : gbc(gridx: 1, gridy : 0)){
                            borderLayout()
                            label(icon : imageIcon('/up_arrow.png'), constraints : BorderLayout.CENTER)
                            label(text : bind { formatSize(model.upSpeed, "B_SEC") }, constraints : BorderLayout.EAST)
                        }
                    }
                    panel (constraints : BorderLayout.EAST) {
                        label(icon : imageIcon("/email.png"))
                        label(text : bind {model.messages})
                        label("   " + trans("CONNECTIONS") + ":")
                        label(text : bind {model.connections})
                    }
                }

            }
        }
        
        collectionsTable = builder.getVariable("collections-table")
        collectionFilesTable = builder.getVariable("items-table")
        
        systemMessageFolderList = builder.getVariable("message-folders-list")
        userMessageFolderList = builder.getVariable("user-message-folders-list")
        messageFolderContents = builder.getVariable("message-folder-contents")
    }

    void mvcGroupInit(Map<String, String> args) {

        def mainFrame = builder.getVariable("main-frame")

        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                chatNotificator.mainWindowDeactivated()
                if (application.getContext().get("tray-icon") != null) {
                    if (settings.closeWarning) {
                        runInsideUIAsync {
                            Map<String, Object> args2 = new HashMap<>()
                            args2.put("settings", settings)
                            args2.put("home", model.core.home)
                            mvcGroup.createMVCGroup("close-warning", "Close Warning", args2)
                        }
                    } else if (settings.exitOnClose)
                        closeApplication()
                } else {
                    closeApplication()
                }
            }

            public void windowDeactivated(WindowEvent e) {
                chatNotificator.mainWindowDeactivated()
            }

            public void windowActivated(WindowEvent e) {
                if (!model.chatPaneButtonEnabled)
                    chatNotificator.mainWindowActivated()
            }
        })

        // search field
        def searchField = builder.getVariable("search-field")

        // downloads table
        def downloadsTable = builder.getVariable("downloads-table")
        def selectionModel = downloadsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener({
            def downloadDetailsPanel = builder.getVariable("download-details-panel")
            int []selectedRows = selectedDownloaderRows()
            if (selectedRows.length == 0) {
                model.cancelButtonEnabled = false
                model.retryButtonEnabled = false
                model.pauseButtonEnabled = false
                model.previewButtonEnabled = false
                model.downloader = null
                downloadDetailsPanel.getLayout().show(downloadDetailsPanel, "select-download")
                return
            }
            
            // simple case, 1 selected download
            if (selectedRows.length == 1) {
                def downloader = model.downloads[selectedRows[0]]?.downloader
                if (downloader == null)
                    return
                model.downloader = downloader
                model.previewButtonEnabled = true
                downloadDetailsPanel.getLayout().show(downloadDetailsPanel, "download-selected")
            
            
                switch (downloader.getCurrentState()) {
                    case Downloader.DownloadState.CONNECTING:
                    case Downloader.DownloadState.DOWNLOADING:
                    case Downloader.DownloadState.HASHLIST:
                        model.cancelButtonEnabled = true
                        model.pauseButtonEnabled = true
                        model.retryButtonEnabled = false
                        break
                    case Downloader.DownloadState.FAILED:
                        model.cancelButtonEnabled = true
                        model.retryButtonEnabled = true
                        model.resumeButtonText = "RETRY"
                        model.pauseButtonEnabled = false
                        break
                    case Downloader.DownloadState.PAUSED:
                        model.cancelButtonEnabled = true
                        model.retryButtonEnabled = true
                        model.resumeButtonText = "RESUME"
                        model.pauseButtonEnabled = false
                        break
                    default:
                        model.cancelButtonEnabled = false
                        model.retryButtonEnabled = false
                        model.pauseButtonEnabled = false
                }
                return
            }
            
            // multiple selected downloads
            downloadDetailsPanel.getLayout().show(downloadDetailsPanel, "select-download")
            model.downloader = null
            
            List<Downloader> downloaders = []
            for (int row : selectedRows)
                downloaders << model.downloads[row].downloader
            
            // default is best guess
            model.previewButtonEnabled = false
            model.cancelButtonEnabled = true
            model.pauseButtonEnabled = true
            model.retryButtonEnabled = false
            
            boolean allPaused = true
            downloaders.each { allPaused &= it.getCurrentState() == Downloader.DownloadState.PAUSED}
            boolean allFailed = true
            downloaders.each { allFailed &= it.getCurrentState() == Downloader.DownloadState.FAILED}
            boolean allFinished = true
            downloaders.each { allFinished &= it.getCurrentState() == Downloader.DownloadState.FINISHED}
            
            if (allPaused) {
                model.pauseButtonEnabled = false
                model.retryButtonEnabled = true
                model.resumeButtonText = "RESUME"
            }
            if (allFailed) {
                model.retryButtonEnabled = true
                model.pauseButtonEnabled = false
                model.resumeButtonText = "RETRY"
            }
            if (allFinished) {
                model.retryButtonEnabled = false
                model.pauseButtonEnabled = false
            }
        })

        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        downloadsTable.setDefaultRenderer(Integer.class, centerRenderer)
        downloadsTable.setDefaultRenderer(Downloader.class, new DownloadProgressRenderer())

        downloadsTable.rowSorter.addRowSorterListener({ evt -> lastDownloadSortEvent = evt })
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

        def sharedFilesMouseListener = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showSharedFilesPopupMenu(e)
                else if (e.getClickCount() == 2)
                    controller.open()
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showSharedFilesPopupMenu(e)
                else if (e.getClickCount() == 2)
                    controller.open()
            }
        }

        // shared files table and tree
        def sharedFilesTable = builder.getVariable("shared-files-table")
        sharedFilesTable.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())

        sharedFilesTable.rowSorter.addRowSorterListener({ evt -> lastSharedSortEvent = evt })
        sharedFilesTable.rowSorter.setSortsOnUpdates(true)

        sharedFilesTable.addMouseListener(sharedFilesMouseListener)

        selectionModel = sharedFilesTable.getSelectionModel()
        selectionModel.addListSelectionListener({
            def selectedFiles = selectedSharedFiles()
            if (selectedFiles == null || selectedFiles.isEmpty()) {
                return
            }
            model.addCommentButtonEnabled = true
            model.publishButtonEnabled = true
            boolean unpublish = true
            selectedFiles.each {
                unpublish &= it?.isPublished()
            }
            model.publishButtonText = unpublish ? "UNPUBLISH" : "PUBLISH"
        })

        JTree sharedFilesTree = builder.getVariable("shared-files-tree")
        sharedFilesTree.addMouseListener(sharedFilesMouseListener)

        sharedFilesTree.addTreeSelectionListener({
            def selectedNode = sharedFilesTree.getLastSelectedPathComponent()
            model.addCommentButtonEnabled = selectedNode != null
            model.publishButtonEnabled = selectedNode != null
        })

        sharedFilesTree.addTreeExpansionListener(expansionListener)


        // collections table
        collectionsTable.setDefaultRenderer(Integer.class, centerRenderer)
        collectionsTable.columnModel.getColumn(3).setCellRenderer(new SizeRenderer())
        collectionsTable.columnModel.getColumn(6).setCellRenderer(new DateRenderer())

        collectionsTable.rowSorter.addRowSorterListener({ evt -> lastCollectionSortEvent = evt })

        selectionModel = collectionsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = selectedCollectionRow()
            if (selectedRow < 0) {
                model.viewCollectionCommentButtonEnabled = false
                model.deleteCollectionButtonEnabled = false
                return
            }

            model.deleteCollectionButtonEnabled = true
            FileCollection collection = model.localCollections.get(selectedRow)
            model.viewCollectionCommentButtonEnabled = collection.getComment() != ""

            model.collectionFiles.clear()
            collection.files.each {
                SharedFile sf = model.core.fileManager.getRootToFiles().get(it.infoHash)[0]
                model.collectionFiles.add(sf)
            }
            collectionFilesTable.model.fireTableDataChanged()
        })

        collectionsTable.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showCollectionTableMenu(e)
            }

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showCollectionTableMenu(e)
            }
        })


        // collection files table
        collectionFilesTable.setDefaultRenderer(Long.class, new SizeRenderer())
        collectionFilesTable.rowSorter.addRowSorterListener({ evt -> lastCollectionFilesSortEvent = evt })
        collectionFilesTable.rowSorter.setSortsOnUpdates(true)
        selectionModel = collectionFilesTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = selectedItemRow()
            if (selectedRow < 0) {
                model.viewItemCommentButtonEnabled = false
                return
            }
            SharedFile sf = model.collectionFiles.get(selectedRow)
            model.viewItemCommentButtonEnabled = sf.getComment() != null
        })

        collectionFilesTable.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showItemsMenu(e)
            }

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showItemsMenu(e)
            }
        })

        // uploadsTable
        def uploadsTable = builder.getVariable("uploads-table")

        uploadsTable.rowSorter.addRowSorterListener({ evt -> lastUploadsSortEvent = evt })
        uploadsTable.rowSorter.setSortsOnUpdates(true)

        selectionModel = uploadsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        uploadsTable.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    showUploadsMenu(e)
            }

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    showUploadsMenu(e)
            }
        })

        // searches table
        def searchesTable = builder.getVariable("searches-table")
        JPopupMenu searchTableMenu = new JPopupMenu()

        JMenuItem copySearchToClipboard = new JMenuItem(trans("COPY_SEARCH_TO_CLIPBOARD"))
        copySearchToClipboard.addActionListener({ mvcGroup.view.copySearchToClipboard(searchesTable) })
        JMenuItem trustSearcher = new JMenuItem(trans("TRUST_SEARCHER"))
        trustSearcher.addActionListener({ mvcGroup.controller.trustPersonaFromSearch() })
        JMenuItem distrustSearcher = new JMenuItem(trans("DISTRUST_SEARCHER"))
        distrustSearcher.addActionListener({ mvcGroup.controller.distrustPersonaFromSearch() })

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

        // feeds table
        def feedsTable = builder.getVariable("feeds-table")
        feedsTable.rowSorter.addRowSorterListener({ evt -> lastFeedsSortEvent = evt })
        feedsTable.rowSorter.setSortsOnUpdates(true)
        feedsTable.setDefaultRenderer(Integer.class, centerRenderer)
        feedsTable.setDefaultRenderer(Long.class, new DateRenderer())
        selectionModel = feedsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            Feed selectedFeed = selectedFeed()
            if (selectedFeed == null) {
                model.updateFileFeedButtonEnabled = false
                model.unsubscribeFileFeedButtonEnabled = false
                model.configureFileFeedButtonEnabled = false
                return
            }

            model.unsubscribeFileFeedButtonEnabled = true
            model.configureFileFeedButtonEnabled = true
            model.updateFileFeedButtonEnabled = !selectedFeed.getStatus().isActive()

            def items = model.core.feedManager.getFeedItems(selectedFeed.getPublisher())
            model.feedItems.clear()
            model.feedItems.addAll(items)

            def feedItemsTable = builder.getVariable("feed-items-table")
            int selectedItemRow = feedItemsTable.getSelectedRow()
            feedItemsTable.model.fireTableDataChanged()
            if (selectedItemRow >= 0 && selectedItemRow < items.size())
                feedItemsTable.selectionModel.setSelectionInterval(selectedItemRow, selectedItemRow)
        })
        feedsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3)
                    showFeedsPopupMenu(e)
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3)
                    showFeedsPopupMenu(e)
            }
        })


        // feed items table
        def feedItemsTable = builder.getVariable("feed-items-table")
        feedItemsTable.rowSorter.addRowSorterListener({ evt -> lastFeedItemsSortEvent = evt })
        feedItemsTable.rowSorter.setSortsOnUpdates(true)
        feedItemsTable.setDefaultRenderer(Integer.class, centerRenderer)
        feedItemsTable.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())
        feedItemsTable.columnModel.getColumn(5).setCellRenderer(new DateRenderer())

        selectionModel = feedItemsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener({
            List<FeedItem> selectedItems = selectedFeedItems()
            if (selectedItems == null || selectedItems.isEmpty()) {
                model.downloadFeedItemButtonEnabled = false
                model.viewFeedItemCommentButtonEnabled = false
                model.viewFeedItemCertificatesButtonEnabled = false
                return
            }
            model.downloadFeedItemButtonEnabled = true
            model.viewFeedItemCommentButtonEnabled = false
            model.viewFeedItemCertificatesButtonEnabled = false
            if (selectedItems.size() == 1) {
                FeedItem item = selectedItems.get(0)
                model.viewFeedItemCommentButtonEnabled = item.getComment() != null
                model.viewFeedItemCertificatesButtonEnabled = item.getCertificates() > 0
            }
        })
        feedItemsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                List<FeedItem> selectedItems = selectedFeedItems()
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3)
                    showFeedItemsPopupMenu(e)
                else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2 &&
                        selectedItems != null && selectedItems.size() == 1 &&
                        model.canDownload(selectedItems.get(0).getInfoHash())) {
                    mvcGroup.controller.downloadFeedItem()
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3)
                    showFeedItemsPopupMenu(e)
            }
        })

        // subscription table
        def subscriptionTable = builder.getVariable("subscription-table")
        subscriptionTable.setDefaultRenderer(Integer.class, centerRenderer)
        subscriptionTable.rowSorter.addRowSorterListener({ evt -> lastContactsSubscriptionSortEvent = evt })
        subscriptionTable.rowSorter.setSortsOnUpdates(true)
        selectionModel = subscriptionTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = getSelectedContactSubscriptionTableRow()
            if (selectedRow < 0) {
                model.reviewButtonEnabled = false
                model.updateButtonEnabled = false
                model.unsubscribeButtonEnabled = false
                return
            }
            def trustList = model.subscriptions[selectedRow]
            if (trustList == null)
                return
            switch (trustList.status) {
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

        subscriptionTable.setDefaultRenderer(Long.class, new DateRenderer())

        // contacts table
        def contactsTable = builder.getVariable("contacts-table")
        contactsTable.rowSorter.addRowSorterListener({ evt -> lastContactsSortEvent = evt })
        contactsTable.rowSorter.setSortsOnUpdates(true)
        selectionModel = contactsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = getSelectedContactsTableRow()
            if (selectedRow < 0) {
                model.subscribeButtonEnabled = false
                model.markDistrustedButtonEnabled = false
                model.chatFromTrustedButtonEnabled = false
                model.browseFromTrustedButtonEnabled = false
                model.messageFromTrustedButtonEnabled = false
                model.removeContactButtonEnabled = false
                model.markTrustedButtonEnabled = false
                return
            }
            model.removeContactButtonEnabled = true

            TrustLevel level = model.core.trustService.getLevel(model.contacts[selectedRow].persona.destination)
            switch (level) {
                case TrustLevel.TRUSTED:
                    model.subscribeButtonEnabled = true
                    model.markDistrustedButtonEnabled = true
                    model.markTrustedButtonEnabled = false
                    model.chatFromTrustedButtonEnabled = true
                    model.browseFromTrustedButtonEnabled = true
                    model.messageFromTrustedButtonEnabled = true
                    break
                case TrustLevel.DISTRUSTED :
                    model.subscribeButtonEnabled = false
                    model.markTrustedButtonEnabled = true
                    model.markDistrustedButtonEnabled = false
                    model.chatFromTrustedButtonEnabled = false
                    model.browseFromTrustedButtonEnabled = false
                    model.messageFromTrustedButtonEnabled = false
                    break
                case TrustLevel.NEUTRAL :
                    throw new IllegalStateException()
            }
    
        })
        
        contactsTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    showContactsMenu(e)
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    showContactsMenu(e)
            }
        })

        // messages tab
        def folderTransferHandler = new FolderImportTransferHandler()
        systemMessageFolderList.setDropMode(DropMode.ON)
        systemMessageFolderList.setTransferHandler(folderTransferHandler)
        systemMessageFolderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        systemMessageFolderList.addListSelectionListener({
            int index = systemMessageFolderList.getSelectedIndex()
            if (index < 0)
                return
            model.folderIdx = model.messageFolders[index].model.name
            messageFolderContents.getLayout().show(messageFolderContents, model.folderIdx)
            userMessageFolderList.clearSelection()
            model.deleteMessageFolderButtonEnabled = false
        })
        
        userMessageFolderList.setDropMode(DropMode.ON)
        userMessageFolderList.setTransferHandler(folderTransferHandler)
        userMessageFolderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        userMessageFolderList.addListSelectionListener({
            int index = userMessageFolderList.getSelectedIndex()
            if (index < 0) {
                model.deleteMessageFolderButtonEnabled = false
                return
            }
            model.folderIdx = model.messageFolders[index + Messenger.RESERVED_FOLDERS.size()].model.name
            messageFolderContents.getLayout().show(messageFolderContents, model.folderIdx)
            systemMessageFolderList.clearSelection()
            model.deleteMessageFolderButtonEnabled = true
        })
        
        JPopupMenu folderMenu = new JPopupMenu()
        JMenuItem deleteItem = new JMenuItem(trans("DELETE_FOLDER"))
        deleteItem.addActionListener({controller.deleteMessageFolder()})
        folderMenu.add(deleteItem)
        
        userMessageFolderList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    showPopupMenu(folderMenu, e)
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    showPopupMenu(folderMenu, e)
            }
        })
        
        // chat tabs
        def chatTabbedPane = builder.getVariable("chat-tabs")
        chatTabbedPane.addChangeListener({e -> chatNotificator.serverTabChanged(e.getSource())})
        
        // result tabs listener
        def resultsTabbedPane = builder.getVariable("result-tabs")
        resultsTabListener = new ResultTabsChangeListener(resultsTabbedPane)
        resultsTabbedPane.addChangeListener(resultsTabListener)
        
        // show tree by default
        showSharedFilesTree.call()
        
        // show search panel by default
        showSearchWindow.call()
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
                TreeUtil.getLeafs(path.getLastPathComponent(), rv)
            }
            return rv
        }
    }
    
    Set<File> selectedFolders() {
        if (!model.treeVisible) 
            return Collections.emptySet()
        
        def sharedFilesTree = builder.getVariable("shared-files-tree")
        TreePath[] selectedPaths = sharedFilesTree.getSelectionPaths()
        Set<File> rv = new HashSet<>()
        for (TreePath path : selectedPaths) {
            DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) path.getLastPathComponent()
            if (!(dmtn.getUserObject() instanceof InterimTreeNode))
                continue
            InterimTreeNode node = (InterimTreeNode) dmtn.getUserObject()
            rv << node.getFile()
        }
        rv
    }
    
    def copyHashToClipboard() {
        def selectedFiles = selectedSharedFiles()
        if (selectedFiles == null)
            return
        String roots = ""
        for (Iterator<SharedFile> iterator = selectedFiles.iterator(); iterator.hasNext(); ) {
            SharedFile selected = iterator.next()
            String root = Base64.encode(selected.getRoot())
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

    int[] selectedDownloaderRows() {
        def downloadsTable = builder.getVariable("downloads-table")
        int [] selected = downloadsTable.getSelectedRows()
        if (selected == null)
            return new int[0]
        if (selected.length == 0)
            return selected
        if (lastDownloadSortEvent != null) {
            for (int i = 0; i < selected.length; i++)
                selected[i] = downloadsTable.rowSorter.convertRowIndexToModel(selected[i])
        }
            
        selected
    }

    def showDownloadsMenu(MouseEvent e) {
        int[] selected = selectedDownloaderRows()
        if (selected.length == 0)
            return
        
        boolean pauseEnabled = false
        boolean cancelEnabled = false
        boolean retryEnabled = false
        String resumeText = "RETRY"
        boolean openFolderEnabled = false
        if (selected.length == 1) {
            Downloader downloader = model.downloads[selected[0]].downloader
            switch (downloader.currentState) {
                case Downloader.DownloadState.FINISHED:
                    openFolderEnabled = true
                    break
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
                    resumeText = "RESUME"
                    break
                default:
                    pauseEnabled = false
                    cancelEnabled = false
                    retryEnabled = false
            }
        } else {
            List<Downloader> downloaders = []
            for (int row : selected)
                downloaders << model.downloads[row].downloader
            
            // default is best guess
            pauseEnabled = true
            cancelEnabled = true
            retryEnabled = true
            openFolderEnabled = false
            resumeText = "RETRY"

            boolean allPaused = true
            downloaders.each { allPaused &= it.getCurrentState() == Downloader.DownloadState.PAUSED}
            boolean allFailed = true
            downloaders.each { allFailed &= it.getCurrentState() == Downloader.DownloadState.FAILED}
            boolean allFinished = true
            downloaders.each { allFinished &= it.getCurrentState() == Downloader.DownloadState.FINISHED}
            
            if (allPaused) {
                pauseEnabled = false
                resumeText = "RESUME"
            }
            if (allFailed) {
                pauseEnabled = false
            }
            if (allFinished) {
                pauseEnabled = false
                retryEnabled = false
            }
        }
        
        JPopupMenu menu = new JPopupMenu()
        JMenuItem copyHashToClipboard = new JMenuItem(trans("COPY_HASH_TO_CLIPBOARD"))
        copyHashToClipboard.addActionListener({
            String hash = Base64.encode(downloader.infoHash.getRoot())
            StringSelection selection = new StringSelection(hash)
            def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
            clipboard.setContents(selection, null)
        })
        menu.add(copyHashToClipboard)

        if (pauseEnabled) {
            JMenuItem pause = new JMenuItem(trans("PAUSE"))
            pause.addActionListener({mvcGroup.controller.pause()})
            menu.add(pause)
        }

        if (cancelEnabled) {
            JMenuItem cancel = new JMenuItem(trans("CANCEL"))
            cancel.addActionListener({mvcGroup.controller.cancel()})
            menu.add(cancel)
        }

        if (retryEnabled) {
            JMenuItem retry = new JMenuItem(trans(resumeText))
            retry.addActionListener({mvcGroup.controller.resume()})
            menu.add(retry)
        }
        
        if (openFolderEnabled) {
            JMenuItem open = new JMenuItem(trans("OPEN_CONTAINING_FOLDER"))
            open.addActionListener({mvcGroup.controller.openContainingFolderFromDownload()})
            menu.add(open)
        }

        showPopupMenu(menu, e)
    }
    
    void showFeedsPopupMenu(MouseEvent e) {
        Feed feed = selectedFeed()
        if (feed == null)
            return
        JPopupMenu menu = new JPopupMenu()
        if (model.updateFileFeedButtonEnabled) {
            JMenuItem update = new JMenuItem(trans("UPDATE"))
            update.addActionListener({mvcGroup.controller.updateFileFeed()})
            menu.add(update)
        }
        
        JMenuItem unsubscribe = new JMenuItem(trans("UNSUBSCRIBE"))
        unsubscribe.addActionListener({mvcGroup.controller.unsubscribeFileFeed()})
        menu.add(unsubscribe)
        
        JMenuItem configure = new JMenuItem(trans("CONFIGURE"))
        configure.addActionListener({mvcGroup.controller.configureFileFeed()})
        menu.add(configure)
        
        JMenuItem copyId = new JMenuItem(trans("COPY_FULL_ID"))
        copyId.addActionListener({controller.copyIdFromFeed()})
        menu.add(copyId)
        
        showPopupMenu(menu,e)
    }
    
    void showFeedItemsPopupMenu(MouseEvent e) {
        List<FeedItem> items = selectedFeedItems()
        if (items == null || items.isEmpty())
            return
        JPopupMenu menu = new JPopupMenu()
        if (model.downloadFeedItemButtonEnabled) {
            JMenuItem download = new JMenuItem(trans("DOWNLOAD"))
            download.addActionListener({mvcGroup.controller.downloadFeedItem()})
            menu.add(download)
        }
        if (model.viewFeedItemCommentButtonEnabled) {
            JMenuItem viewComment = new JMenuItem(trans("VIEW_COMMENT"))
            viewComment.addActionListener({mvcGroup.controller.viewFeedItemComment()})
            menu.add(viewComment)
        }
        if (model.viewFeedItemCertificatesButtonEnabled) {
            JMenuItem viewCertificates = new JMenuItem(trans("VIEW_CERTIFICATES"))
            viewCertificates.addActionListener({mvcGroup.controller.viewFeedItemCertificates()})
            menu.add(viewCertificates)
        }
        showPopupMenu(menu, e)
    }
    
    def selectedUploader() {
        def uploadsTable = builder.getVariable("uploads-table")
        int selectedRow = uploadsTable.getSelectedRow()
        if (selectedRow < 0)
            return null
        if (lastUploadsSortEvent != null) 
            selectedRow = uploadsTable.rowSorter.convertRowIndexToModel(selectedRow)
        model.uploads[selectedRow].uploader
    }
    
    void focusOnSharedFile(SharedFile sf) {
        if(model.treeVisible) {
            def tree = builder.getVariable("shared-files-tree")
            def node = model.fileToNode.get(sf)
            if (node == null)
                return
            
            Object[] path = node.getUserObjectPath()
            DefaultMutableTreeNode otherNode = model.treeRoot
            for (int i = 1; i < path.length; i++) {
                Object o = path[i]
                DefaultMutableTreeNode next = null
                for (int j = 0; j < otherNode.childCount; j++) {
                    if (otherNode.getChildAt(j).getUserObject() == o) {
                        next = otherNode.getChildAt(j)
                        break
                    }
                }
                if (next == null)
                    return // probably filtered from view
                otherNode = next
            }
            
            def otherPath = new TreePath(otherNode.getPath())
            tree.setSelectionPath(otherPath)
            tree.scrollPathToVisible(otherPath)
        } else {
            def table = builder.getVariable("shared-files-table")
            int row = model.shared.indexOf(sf)
            if (row < 0)
                return
            if (lastSharedSortEvent != null)
                row = table.rowSorter.convertRowIndexToView(row)
            table.setRowSelectionInterval(row, row)
            
            table.scrollRectToVisible(new Rectangle(table.getCellRect(row, 0, true)))
        }
    }
    
    void showUploadsMenu(MouseEvent e) {
        Uploader uploader = selectedUploader()
        if (uploader == null)
            return
            
        JPopupMenu uploadsTableMenu = new JPopupMenu()
        JMenuItem showInLibrary = new JMenuItem(trans("SHOW_IN_LIBRARY"))
        showInLibrary.addActionListener({mvcGroup.controller.showInLibrary()})
        uploadsTableMenu.add(showInLibrary)
        
        if (uploader.isBrowseEnabled()) {
            JMenuItem browseItem = new JMenuItem(trans("BROWSE_HOST"))
            browseItem.addActionListener({mvcGroup.controller.browseFromUpload()})
            uploadsTableMenu.add(browseItem)
            JMenuItem browseCollectionsItem = new JMenuItem(trans("BROWSE_COLLECTIONS"))
            browseCollectionsItem.addActionListener({mvcGroup.controller.browseCollectionsFromUpload()})
            uploadsTableMenu.add(browseCollectionsItem)
        }
        
        if (uploader.isFeedEnabled() && mvcGroup.controller.core.feedManager.getFeed(uploader.getDownloaderPersona()) == null) {
            JMenuItem feedItem = new JMenuItem(trans("SUBSCRIBE"))
            feedItem.addActionListener({mvcGroup.controller.subscribeFromUpload()})
            uploadsTableMenu.add(feedItem)
        }
        
        if (uploader.isChatEnabled() && !mvcGroup.controller.core.chatManager.isConnected(uploader.getDownloaderPersona())) {
            JMenuItem chatItem = new JMenuItem(trans("CHAT"))
            chatItem.addActionListener({mvcGroup.controller.chatFromUpload()})
            uploadsTableMenu.add(chatItem)
        }
        
        if (uploader.isMessageEnabled()) {
            JMenuItem messageItem = new JMenuItem(trans("MESSAGE_VERB"))
            messageItem.addActionListener({mvcGroup.controller.messageComposeFromUpload()})
            uploadsTableMenu.add(messageItem)
        }
        
        JMenuItem copyIDItem = new JMenuItem(trans("COPY_FULL_ID"))
        copyIDItem.addActionListener({mvcGroup.controller.copyIdFromUploads()})
        uploadsTableMenu.add(copyIDItem)
        
        showPopupMenu(uploadsTableMenu, e)
    }
    
    void showSharedFilesPopupMenu(MouseEvent e) {
        def selectedFiles = selectedSharedFiles()
        
        JPopupMenu sharedFilesMenu = new JPopupMenu()
        
        if (selectedFiles != null && selectedFiles.size() == 1) {
            JMenuItem openFile = new JMenuItem(trans("OPEN"))
            openFile.addActionListener({mvcGroup.controller.open()})
            sharedFilesMenu.add(openFile)
        }
        
        JMenuItem copyHashToClipboard = new JMenuItem(trans("COPY_HASH_TO_CLIPBOARD"))
        copyHashToClipboard.addActionListener({mvcGroup.view.copyHashToClipboard()})
        sharedFilesMenu.add(copyHashToClipboard)
        
        JMenuItem createCollection = new JMenuItem(trans("CREATE_COLLECTION"))
        createCollection.addActionListener({mvcGroup.controller.collection()})
        sharedFilesMenu.add(createCollection)

        JMenuItem unshareSelectedFiles = new JMenuItem(trans("UNSHARE_SELECTED_FILES"))
        unshareSelectedFiles.addActionListener({mvcGroup.controller.unshareSelectedFile()})
        sharedFilesMenu.add(unshareSelectedFiles)
        JMenuItem commentSelectedFiles = new JMenuItem(trans("COMMENT_SELECTED_FILES"))
        commentSelectedFiles.addActionListener({mvcGroup.controller.addComment()})
        sharedFilesMenu.add(commentSelectedFiles)
        JMenuItem certifySelectedFiles = new JMenuItem(trans("CERTIFY_SELECTED_FILES"))
        certifySelectedFiles.addActionListener({mvcGroup.controller.issueCertificate()})
        sharedFilesMenu.add(certifySelectedFiles)
        JMenuItem openContainingFolder = new JMenuItem(trans("OPEN_CONTAINING_FOLDER"))
        openContainingFolder.addActionListener({mvcGroup.controller.openContainingFolder()})
        sharedFilesMenu.add(openContainingFolder)
        JMenuItem showFileDetails = new JMenuItem(trans("SHOW_FILE_DETAILS"))
        showFileDetails.addActionListener({mvcGroup.controller.showFileDetails()})
        sharedFilesMenu.add(showFileDetails)
        
        showPopupMenu(sharedFilesMenu, e)
    }
    
    void showRestoreOrEmpty() {
        def searchWindow = builder.getVariable("search window")
        String id
        if (!model.sessionRestored && !settings.openTabs.isEmpty())
            id = model.connections > 0 ? "restore session" : "tabs-panel"
        else
            id = "tabs-panel"
        searchWindow.getLayout().show(searchWindow, id)
    }

    def showSearchWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "search window")

        showRestoreOrEmpty()        
        
        model.searchesPaneButtonEnabled = false
        model.downloadsPaneButtonEnabled = true
        model.uploadsPaneButtonEnabled = true
        model.collectionsPaneButtonEnabled = true
        model.monitorPaneButtonEnabled = true
        model.feedsPaneButtonEnabled = true
        model.trustPaneButtonEnabled = true
        model.messagesPaneButtonEnabled = true
        model.chatPaneButtonEnabled = true
        chatNotificator.mainWindowDeactivated()
        resultsTabListener.markSelectedVisible()
    }

    def showDownloadsWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "downloads window")
        model.searchesPaneButtonEnabled = true
        model.downloadsPaneButtonEnabled = false
        model.uploadsPaneButtonEnabled = true
        model.collectionsPaneButtonEnabled = true
        model.monitorPaneButtonEnabled = true
        model.feedsPaneButtonEnabled = true
        model.trustPaneButtonEnabled = true
        model.messagesPaneButtonEnabled = true
        model.chatPaneButtonEnabled = true
        chatNotificator.mainWindowDeactivated()
        resultsTabListener.markAllTabsInvisible()
    }

    def showUploadsWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "uploads window")
        model.searchesPaneButtonEnabled = true
        model.downloadsPaneButtonEnabled = true
        model.uploadsPaneButtonEnabled = false
        model.collectionsPaneButtonEnabled = true
        model.monitorPaneButtonEnabled = true
        model.feedsPaneButtonEnabled = true
        model.trustPaneButtonEnabled = true
        model.messagesPaneButtonEnabled = true
        model.chatPaneButtonEnabled = true
        chatNotificator.mainWindowDeactivated()
        resultsTabListener.markAllTabsInvisible()
    }
    
    def showCollectionsWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "collections window")
        model.searchesPaneButtonEnabled = true
        model.downloadsPaneButtonEnabled = true
        model.uploadsPaneButtonEnabled = true
        model.collectionsPaneButtonEnabled = false
        model.monitorPaneButtonEnabled = true
        model.feedsPaneButtonEnabled = true
        model.trustPaneButtonEnabled = true
        model.messagesPaneButtonEnabled = true
        model.chatPaneButtonEnabled = true
        chatNotificator.mainWindowDeactivated()
        resultsTabListener.markAllTabsInvisible()
    }

    def showMonitorWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel,"monitor window")
        model.searchesPaneButtonEnabled = true
        model.downloadsPaneButtonEnabled = true
        model.uploadsPaneButtonEnabled = true
        model.collectionsPaneButtonEnabled = true
        model.monitorPaneButtonEnabled = false
        model.feedsPaneButtonEnabled = true
        model.trustPaneButtonEnabled = true
        model.messagesPaneButtonEnabled = true
        model.chatPaneButtonEnabled = true
        chatNotificator.mainWindowDeactivated()
        resultsTabListener.markAllTabsInvisible()
    }
    
    def showFeedsWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel,"feeds window")
        model.searchesPaneButtonEnabled = true
        model.downloadsPaneButtonEnabled = true
        model.uploadsPaneButtonEnabled = true
        model.collectionsPaneButtonEnabled = true
        model.monitorPaneButtonEnabled = true
        model.feedsPaneButtonEnabled = false
        model.trustPaneButtonEnabled = true
        model.messagesPaneButtonEnabled = true
        model.chatPaneButtonEnabled = true
        chatNotificator.mainWindowDeactivated()
        resultsTabListener.markAllTabsInvisible()
    }

    def showTrustWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel,"trust window")
        model.searchesPaneButtonEnabled = true
        model.downloadsPaneButtonEnabled = true
        model.uploadsPaneButtonEnabled = true
        model.collectionsPaneButtonEnabled = true
        model.monitorPaneButtonEnabled = true
        model.feedsPaneButtonEnabled = true
        model.trustPaneButtonEnabled = false
        model.messagesPaneButtonEnabled = true
        model.chatPaneButtonEnabled = true
        chatNotificator.mainWindowDeactivated()
        resultsTabListener.markAllTabsInvisible()
    }
    
    def showMessagesWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "messages window")
        model.searchesPaneButtonEnabled = true
        model.downloadsPaneButtonEnabled = true
        model.uploadsPaneButtonEnabled = true
        model.collectionsPaneButtonEnabled = true
        model.monitorPaneButtonEnabled = true
        model.feedsPaneButtonEnabled = true
        model.trustPaneButtonEnabled = true
        model.messagesPaneButtonEnabled = false
        model.chatPaneButtonEnabled = true
        chatNotificator.mainWindowDeactivated()
        resultsTabListener.markAllTabsInvisible()
    }
    
    def showChatWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "chat window")
        model.searchesPaneButtonEnabled = true
        model.downloadsPaneButtonEnabled = true
        model.uploadsPaneButtonEnabled = true
        model.collectionsPaneButtonEnabled = true
        model.monitorPaneButtonEnabled = true
        model.feedsPaneButtonEnabled = true
        model.trustPaneButtonEnabled = true
        model.messagesPaneButtonEnabled = true
        model.chatPaneButtonEnabled = false
        chatNotificator.mainWindowActivated()
        resultsTabListener.markAllTabsInvisible()
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
        chooser.setDialogTitle(trans("SELECT_FILES_OR_DIRECTORIES_TO_SHARE"))
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES)
        chooser.setMultiSelectionEnabled(true)
        int rv = chooser.showOpenDialog(null)
        if (rv == JFileChooser.APPROVE_OPTION) {
            chooser.getSelectedFiles().each {
                File absolute = it.getAbsoluteFile()
                model.core.negativeFiles.negativeTree.remove(absolute)
                model.core.eventBus.publish(new FileSharedEvent(file : absolute))
            }
        }
    }
    
    void addSystemMessageFolder(MVCGroup group) {
        model.messageFolders.add(group)
        model.messageFoldersMap.put(group.model.name, group)
        model.messageFolderListModel.addElement(trans(group.model.txKey))

        messageFolderContents.add(group.view.folderPanel, group.model.name)
    }
    
    void addUserMessageFolder(MVCGroup group) {
        model.messageFolders.add(group)
        model.messageFoldersMap.put(group.model.name, group)
        model.userMessageFolderListModel.addElement(group.model.name)

        messageFolderContents.add(group.view.folderPanel, group.model.name)
    }
    
    void deleteUserMessageFolder(String name) {
        def group = model.messageFoldersMap.remove(name)
        if (group == null)
            return
        model.userMessageFolderListModel.removeElement(name)
        messageFolderContents.remove(group.view.folderPanel)
    }

    int getSelectedContactsTableRow() {
        def table = builder.getVariable("contacts-table")
        int selectedRow = table.getSelectedRow()
        if (selectedRow < 0)
            return -1
        if (lastContactsSortEvent != null)
            selectedRow = table.rowSorter.convertRowIndexToModel(selectedRow)
        selectedRow
    }
    
    int getSelectedContactSubscriptionTableRow() {
        def table = builder.getVariable("subscription-table")
        int selectedRow = table.getSelectedRow()
        if (selectedRow < 0)
            return -1
        if (lastContactsSubscriptionSortEvent != null)
            selectedRow = table.rowSorter.convertRowIndexToModel(selectedRow)
        selectedRow
    }
    
    void clearSelectedFiles() {
        JTree tree = builder.getVariable("shared-files-tree")
        tree.setSelectionPaths(new TreePath[0])
        expansionListener.expandedPaths.clear()
        expansionListener.manualExpansion = false
        JTable table = builder.getVariable("shared-files-table")
        table.selectionModel.clearSelection()
    }
    
    public void refreshSharedFiles() {
        refreshSharedFilesTree()
        refreshSharedFilesTable()
    }
    
    void refreshSharedFilesTree() {
        def tree = builder.getVariable("shared-files-tree")
        TreePath[] selectedPaths = tree.getSelectionPaths()
        Set<TreePath> expanded = new HashSet<>(expansionListener.expandedPaths)
        
        model.sharedTree.nodeStructureChanged(model.treeRoot)
        
        expanded.each { tree.expandPath(it) }
        tree.setSelectionPaths(selectedPaths)
    }
    
    void refreshSharedFilesTable() {
        def table = builder.getVariable("shared-files-table")
        int [] selectedRows = table.getSelectedRows()
        table.model.fireTableDataChanged()
        for (int row : selectedRows)
            table.selectionModel.addSelectionInterval(row, row)
    }
    
    public void refreshFeeds() {
        JTable feedsTable = builder.getVariable("feeds-table")
        int selectedFeed = feedsTable.getSelectedRow()
        feedsTable.model.fireTableDataChanged()
        if (selectedFeed >= 0)
            feedsTable.selectionModel.setSelectionInterval(selectedFeed, selectedFeed)
        
        JTable feedItemsTable = builder.getVariable("feed-items-table")
        feedItemsTable.model.fireTableDataChanged()
    }
    
    Feed selectedFeed() {
        JTable feedsTable = builder.getVariable("feeds-table")
        int row = feedsTable.getSelectedRow()
        if (row < 0)
            return null
        if (lastFeedsSortEvent != null)
            row = feedsTable.rowSorter.convertRowIndexToModel(row)
        model.feeds[row]
    }
    
    List<FeedItem> selectedFeedItems() {
        JTable feedItemsTable = builder.getVariable("feed-items-table")
        int [] selectedRows = feedItemsTable.getSelectedRows()
        if (selectedRows.length == 0)
            return null
        List<FeedItem> rv = new ArrayList<>()
        if (lastFeedItemsSortEvent != null) {
            for (int i = 0; i < selectedRows.length; i++) {
                selectedRows[i] = feedItemsTable.rowSorter.convertRowIndexToModel(selectedRows[i])
            }
        }
        for (int selectedRow : selectedRows)
            rv.add(model.feedItems[selectedRow])
        rv
    }
    
    int selectedCollectionRow() {
        int selectedRow = collectionsTable.getSelectedRow()
        if (selectedRow < 0)
            return -1
        if (lastCollectionSortEvent != null)
            selectedRow = collectionsTable.rowSorter.convertRowIndexToModel(selectedRow)
        selectedRow
    }
    
    int selectedItemRow() {
        int selectedRow = collectionFilesTable.getSelectedRow()
        if (selectedRow < 0)
            return -1
        if (lastCollectionFilesSortEvent != null)
            selectedRow = collectionFilesTable.rowSorter.convertRowIndexToModel(selectedRow)
        selectedRow
    }
    
    private void showCollectionTableMenu(MouseEvent e) {
        int row = selectedCollectionRow()
        if (row < 0)
            return
        FileCollection collection = model.localCollections.get(row)
        
        JPopupMenu menu = new JPopupMenu()
        JMenuItem copyHashToClipboard = new JMenuItem(trans("COPY_HASH_TO_CLIPBOARD"))
        copyHashToClipboard.addActionListener({controller.copyCollectionHash()})
        menu.add(copyHashToClipboard)
        
        if (collection.comment != "") {
            JMenuItem viewComment = new JMenuItem(trans("VIEW_COMMENT"))
            viewComment.addActionListener({controller.viewCollectionComment()})
            menu.add(viewComment)
        }
            
        JMenuItem delete = new JMenuItem(trans("DELETE"))
        delete.addActionListener({controller.deleteCollection()})
        menu.add(delete)
        
        JMenuItem showHits = new JMenuItem(trans("COLLECTION_SHOW_HITS"))
        showHits.addActionListener({controller.showCollectionTool()})
        menu.add(showHits)
        
        showPopupMenu(menu, e)
    }
    
    private void showItemsMenu(MouseEvent e) {
        int row = selectedItemRow()
        if (row < 0)
            return
        SharedFile item = model.collectionFiles.get(row)
        if (item.getComment() == null || item.getComment() == "")
            return
        JPopupMenu menu = new JPopupMenu()
        JMenuItem viewComment = new JMenuItem(trans("VIEW_COMMENT"))
        viewComment.addActionListener({controller.viewItemComment()})
        menu.add(viewComment)
        showPopupMenu(menu, e)
    }
    
    void showContactsMenu(MouseEvent e) {
        JPopupMenu trustMenu = new JPopupMenu()
        if (model.subscribeButtonEnabled) {
            JMenuItem subscribeItem = new JMenuItem(trans("SUBSCRIBE"))
            subscribeItem.addActionListener({ mvcGroup.controller.subscribe() })
            trustMenu.add(subscribeItem)
        }
        JMenuItem markNeutralItem = new JMenuItem(trans("REMOVE_CONTACT"))
        markNeutralItem.addActionListener({mvcGroup.controller.removeContact()})
        trustMenu.add(markNeutralItem)
        if (model.markDistrustedButtonEnabled) {
            JMenuItem markDistrustedItem = new JMenuItem(trans("MARK_DISTRUSTED"))
            markDistrustedItem.addActionListener({ mvcGroup.controller.markDistrusted() })
            trustMenu.add(markDistrustedItem)
        }
        if (model.markTrustedButtonEnabled) {
            JMenuItem markTrustedItem = new JMenuItem(trans("MARK_TRUSTED"))
            markTrustedItem.addActionListener({mvcGroup.controller.markTrusted()})
            trustMenu.add(markTrustedItem)
        }
        if (model.browseFromTrustedButtonEnabled) {
            JMenuItem browseItem = new JMenuItem(trans("BROWSE"))
            browseItem.addActionListener({ mvcGroup.controller.browseFromTrusted() })
            trustMenu.add(browseItem)
            JMenuItem browseCollectionsItem = new JMenuItem(trans("BROWSE_COLLECTIONS"))
            browseCollectionsItem.addActionListener({ mvcGroup.controller.browseCollectionsFromTrusted() })
            trustMenu.add(browseCollectionsItem)
        }
        if (model.chatFromTrustedButtonEnabled) {
            JMenuItem chatItem = new JMenuItem(trans("CHAT"))
            chatItem.addActionListener({ mvcGroup.controller.chatFromTrusted() })
            trustMenu.add(chatItem)
        }
        if (model.messageFromTrustedButtonEnabled) {
            JMenuItem messageItem = new JMenuItem(trans("MESSAGE_VERB"))
            messageItem.addActionListener({ mvcGroup.controller.messageFromTrusted() })
            trustMenu.add(messageItem)
        }
        JMenuItem copyIdFromTrustedItem = new JMenuItem(trans("COPY_FULL_ID"))
        copyIdFromTrustedItem.addActionListener({mvcGroup.controller.copyIdFromTrusted()})
        trustMenu.add(copyIdFromTrustedItem)
        
        showPopupMenu(trustMenu, e)
    }
    
    void closeApplication() {
        Core core = application.getContext().get("core")
        
        def tabbedPane = builder.getVariable("result-tabs")
        settings.openTabs.clear()
        int count = tabbedPane.getTabCount()
        for (int i = 0; i < count; i++)
            settings.openTabs.add(tabbedPane.getTitleAt(i))
        settings.openTabs.removeAll(model.browses)
        settings.openTabs.removeAll(model.collections)
            
        JFrame mainFrame = builder.getVariable("main-frame")
        settings.mainFrameX = mainFrame.getSize().width
        settings.mainFrameY = mainFrame.getSize().height
        mainFrame.setVisible(false)
        application.getWindowManager().findWindow("shutdown-window")?.setVisible(true)
        if (core != null) {
            Thread t = new Thread({
                core.shutdown()
                application.shutdown()
            }as Runnable)
            t.start()
            File uiPropsFile = new File(core.home, "gui.properties")
            uiPropsFile.withOutputStream { settings.write(it) }
        }
    }
    
    private class ResultTabsChangeListener implements ChangeListener {
        
        private final JTabbedPane resultTabs
        ResultTabsChangeListener(JTabbedPane resultTabs) {
            this.resultTabs = resultTabs
        }

        void markAllTabsInvisible() {
            // first disable all tabs
            final int count = resultTabs.getTabCount()
            for (int i = 0; i < count; i++) {
                JPanel panel = resultTabs.getComponentAt(i)
                panel.getClientProperty("focusListener")?.onFocus(false)
            }
        }
        
        void markSelectedVisible() {
            markAllTabsInvisible()
            JPanel selected = resultTabs.getSelectedComponent()
            selected?.getClientProperty("focusListener")?.onFocus(true)
        }
        
        @Override
        void stateChanged(ChangeEvent e) {
            markSelectedVisible()
        }
    }

    /**
     * Expands the tree until there is a path with more than one node
     * unless there has been manual expansion already.
     */
    void magicTreeExpansion() {
        if (expansionListener.manualExpansion)
            return
            
        // magic tree expansion like in the plugin
        JTree sharedFilesTree = builder.getVariable("shared-files-tree")
        TreeNode currentNode = model.treeRoot
        int currentRow = 0
        while(currentNode.childCount == 1) {
            sharedFilesTree.expandRow(currentRow++)
            currentNode = currentNode.getChildAt(0)
        }
    }
    
    void fullTreeExpansion() {
        JTree sharedFilesTree = builder.getVariable("shared-files-tree")
        for (int i = 0; i < sharedFilesTree.rowCount; i ++)
            sharedFilesTree.expandRow(i)
    }
    
    private static String formatSize(long size, String suffix) {
        StringBuffer sb = new StringBuffer(32)
        suffix = trans(suffix)
        SizeFormatter.format(size, sb)
        sb.append(suffix)
        sb.toString()
    }

    private class MWTransferHandler extends TransferHandler {
        public boolean canImport(TransferHandler.TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
        }
        public boolean importData(TransferHandler.TransferSupport support) {
            def files = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor)
            files.each {
                File absolute = it.getAbsoluteFile()
                model.core.negativeFiles.negativeTree.remove(absolute)
                model.core.eventBus.publish(new FileSharedEvent(file : absolute))
            }
            showUploadsWindow.call()
            true
        }
        @Override
        protected Transferable createTransferable(JComponent c) {
            if (c instanceof JTree || c instanceof JTable) {
                return new MWTransferable(selectedSharedFiles())
            }
            return null
        }
        @Override
        public int getSourceActions(JComponent c) {
            return LINK | COPY | MOVE
        }
        
        
    }
    
    private class FileCollectionTransferHandler extends TransferHandler {
        @Override
        protected Transferable createTransferable(JComponent c) {
            if (c instanceof JTable) {
                int row = selectedCollectionRow()
                if (row < 0)
                    return null
                return new MWTransferable(Collections.singletonList(model.localCollections.get(row)))
            }
            return null
        }
        @Override
        public int getSourceActions(JComponent c) {
            return LINK | COPY | MOVE
        }
    }
    
    private class PersonaTransferHandler extends TransferHandler {
        @Override
        protected Transferable createTransferable(JComponent c) {
            if (c instanceof JTable) {
                int row = getSelectedContactsTableRow()
                if (row < 0)
                    return null
                return new MWTransferable(Collections.singletonList(model.contacts.get(row).persona))
            }
            return null
        }
        @Override
        public int getSourceActions(JComponent c) {
            return LINK | COPY | MOVE
        }
    }
    
    private class FolderImportTransferHandler extends TransferHandler {
        @Override
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            for (DataFlavor df : transferFlavors) {
                if (df == CopyPasteSupport.LIST_FLAVOR) {
                    return true
                }
            }
            return false
        }

        @Override
        boolean importData(JComponent comp, Transferable t) {

            List<?> items = t.getTransferData(CopyPasteSupport.LIST_FLAVOR)
            if (items == null || items.isEmpty()) {
                return false
            }
            JList target = (JList) comp

            int index = target.getDropLocation().index
            String to

            if (comp == systemMessageFolderList)
                to = model.messageFolders[index].model.name
            else
                to = model.messageFolders[index + Messenger.RESERVED_FOLDERS.size()].model.name

            Set<MWMessage> toRemove = new HashSet<>()
            String from = null
            items.each {
                MWMessageTransferable transferable = it
                from = transferable.from
                def event = new UIMessageMovedEvent(message: transferable.message,
                        from: transferable.from, to: to)
                model.core.eventBus.publish(event)
                toRemove.add(transferable.message)
            }

            MVCGroup fromGroup = null
            model.messageFolders.each {
                if (it.model.name == from) {
                    fromGroup = it
                    return
                }
            }

            fromGroup.view.removeMessages(toRemove)
            return true
        }
    }
}