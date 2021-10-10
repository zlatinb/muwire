package com.muwire.gui

import griffon.core.artifact.GriffonView

import javax.swing.JPanel
import javax.swing.tree.TreePath

import static com.muwire.gui.Translator.trans
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64
import net.i2p.data.DataHelper

import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.search.UIResultEvent
import com.muwire.core.util.DataUtil

import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class SearchTabView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    SearchTabModel model
    @MVCMember @Nonnull
    SearchTabController controller
    
    UISettings settings

    def pane
    def parent
    def searchTerms
    def sendersTable, sendersTable2
    def lastSendersSortEvent
    def resultsTable, resultsTable2
    private JPanel resultsPanel
    private ResultTree resultTree
            
    def lastSortEvent
    def lastResults2SortEvent, lastSenders2SortEvent
    
    def sequentialDownloadCheckbox
    def sequentialDownloadCheckbox2

    void initUI() {
        int rowHeight = application.context.get("row-height")
        pane = builder.panel {
                borderLayout()
                panel (id : "results-panel", constraints : BorderLayout.CENTER) {
                    cardLayout()
                    panel (constraints : "grouped-by-sender"){
                        gridLayout(rows :1, cols : 1)
                        splitPane(orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout : true, dividerLocation: 300 ) {
                            panel {
                                borderLayout()
                                scrollPane (constraints : BorderLayout.CENTER) {
                                    sendersTable = table(id : "senders-table", autoCreateRowSorter : true, rowHeight : rowHeight) {
                                        tableModel(list : model.senders) {
                                            closureColumn(header : trans("SENDER"), preferredWidth : 500, type: String, read : {row -> row.getHumanReadableName()})
                                            closureColumn(header : trans("RESULTS"), preferredWidth : 20, type: Integer, read : {row -> model.sendersBucket[row].size()})
                                            closureColumn(header : trans("BROWSE"), preferredWidth : 20, type: Boolean, read : {row -> model.sendersBucket[row].first().browse})
                                            closureColumn(header : trans("COLLECTIONS"), preferredWidth : 20, type: Boolean, read : {row -> model.sendersBucket[row].first().browseCollections})
                                            closureColumn(header : trans("FEED"), preferredWidth : 20, type : Boolean, read : {row -> model.sendersBucket[row].first().feed})
                                            closureColumn(header : trans("MESSAGES"), preferredWidth : 20, type : Boolean, read : {row -> model.sendersBucket[row].first().messages})
                                            closureColumn(header : trans("CHAT"), preferredWidth : 20, type : Boolean, read : {row -> model.sendersBucket[row].first().chat})
                                            closureColumn(header : trans("TRUST_NOUN"), preferredWidth : 50, type: String, read : { row ->
                                                trans(model.core.trustService.getLevel(row.destination).name())
                                            })
                                        }
                                    }
                                }
                                panel(constraints : BorderLayout.SOUTH) {
                                    gridLayout(rows: 1, cols : 3)
                                    panel (border : etchedBorder()){
                                        button(text : trans("SUBSCRIBE"), enabled : bind {model.subscribeActionEnabled}, subscribeAction)
                                        button(text : trans("MESSAGE_VERB"), enabled : bind{model.messageActionEnabled}, messageAction)
                                        button(text : trans("CHAT"), enabled : bind{model.chatActionEnabled}, chatAction)
                                    }
                                    panel (border : etchedBorder()) {
                                        button(text : trans("BROWSE_HOST"), enabled : bind {model.browseActionEnabled}, browseAction)
                                        button(text : trans("BROWSE_COLLECTIONS"), enabled : bind {model.browseCollectionsActionEnabled}, browseCollectionsAction)
                                    }
                                    panel (border : etchedBorder()){
                                        button(text : trans("ADD_CONTACT"), enabled: bind {model.trustButtonsEnabled }, trustAction)
                                        button(text : trans("DISTRUST"), enabled : bind {model.trustButtonsEnabled}, distrustAction)
                                    }
                                }
                            }
                            panel {
                                borderLayout()
                                resultsPanel = panel(constraints: BorderLayout.CENTER) {
                                    cardLayout()
                                    panel(constraints: "table") {
                                        borderLayout()
                                        scrollPane(constraints: BorderLayout.CENTER) {
                                            resultsTable = table(id: "results-table", autoCreateRowSorter: true, rowHeight: rowHeight) {
                                                tableModel(list: model.results) {
                                                    closureColumn(header: trans("NAME"), preferredWidth: 350, type: String, read: { row -> HTMLSanitizer.sanitize(row.name) })
                                                    closureColumn(header: trans("SIZE"), preferredWidth: 20, type: Long, read: { row -> row.size })
                                                    closureColumn(header: trans("DIRECT_SOURCES"), preferredWidth: 50, type: Integer, read: { row -> model.hashBucket[row.infohash].size() })
                                                    closureColumn(header: trans("POSSIBLE_SOURCES"), preferredWidth: 50, type: Integer, read: { row -> model.sourcesBucket[row.infohash].size() })
                                                    closureColumn(header: trans("COMMENTS"), preferredWidth: 20, type: Boolean, read: { row -> row.comment != null })
                                                    closureColumn(header: trans("CERTIFICATES"), preferredWidth: 20, type: Integer, read: { row -> row.certificates })
                                                    closureColumn(header: trans("COLLECTIONS"), preferredWidth: 20, type: Integer, read: { UIResultEvent row -> row.collections.size() })
                                                }
                                            }
                                        }
                                    }
                                    panel(constraints: "tree") {
                                        borderLayout()
                                        scrollPane(constraints: BorderLayout.CENTER) {
                                            resultTree = new ResultTree(model.treeModel)
                                            tree(id: "results-tree", rowHeight: rowHeight, resultTree)
                                        }
                                    }
                                }
                                panel(constraints : BorderLayout.SOUTH) {
                                    gridBagLayout()
                                    panel(constraints: gbc(gridx: 0, gridy:0, weightx: 100)) {
                                        buttonGroup(id: "viewType")
                                        radioButton(text: trans("TREE"), selected: true, buttonGroup: viewType, actionPerformed: showTree)
                                        radioButton(text: trans("TABLE"), selected: false, buttonGroup: viewType, actionPerformed: showTable)
                                    }
                                    button(text : trans("DOWNLOAD"), enabled : bind {model.downloadActionEnabled}, constraints : gbc(gridx : 1, gridy:0), downloadAction)
                                    button(text : trans("VIEW_COMMENT"), enabled : bind {model.viewCommentActionEnabled}, constraints : gbc(gridx:2, gridy:0),  showCommentAction)
                                    button(text : trans("VIEW_CERTIFICATES"), enabled : bind {model.viewCertificatesActionEnabled}, constraints : gbc(gridx:3, gridy:0), viewCertificatesAction)
                                    button(text : trans("VIEW_COLLECTIONS"), enabled : bind {model.viewCollectionsActionEnabled}, constraints : gbc(gridx:4, gridy:0), viewCollectionsAction)
                                    label(text : trans("DOWNLOAD_SEQUENTIALLY"), constraints : gbc(gridx: 5, gridy: 0, weightx : 80, anchor : GridBagConstraints.LINE_END))
                                    sequentialDownloadCheckbox = checkBox(constraints : gbc(gridx : 6, gridy: 0, anchor : GridBagConstraints.LINE_END),
                                    selected : false, enabled : bind {model.downloadActionEnabled})
                                }
                            }
                        }
                    }
                    panel (constraints : "grouped-by-file") {
                        gridLayout(rows : 1, cols : 1)
                        splitPane(orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout : true, dividerLocation: 300 ) {
                            panel {
                                borderLayout()
                                scrollPane(constraints : BorderLayout.CENTER) {
                                    resultsTable2 = table(id : "results-table2", autoCreateRowSorter : true, rowHeight : rowHeight) {
                                        tableModel(list : model.results2) {
                                            closureColumn(header : trans("NAME"), preferredWidth : 350, type : String, read : {
                                                HTMLSanitizer.sanitize(model.hashBucket[it].first().name)
                                            })
                                            closureColumn(header : trans("SIZE"), preferredWidth : 20, type : Long, read : {
                                                model.hashBucket[it].first().size
                                            })
                                            closureColumn(header : trans("DIRECT_SOURCES"), preferredWidth : 20, type : Integer, read : {
                                                model.hashBucket[it].size()
                                            })
                                            closureColumn(header : trans("POSSIBLE_SOURCES"), preferredWidth : 20, type : Integer , read : {
                                                model.sourcesBucket[it].size()
                                            })
                                            closureColumn(header : trans("COMMENTS"), preferredWidth : 20, type : Integer, read : {
                                                int count = 0
                                                model.hashBucket[it].each { 
                                                    if (it.comment != null)
                                                        count++
                                                }
                                                count
                                            })
                                            closureColumn(header : trans("CERTIFICATES"), preferredWidth : 20, type : Integer, read : {
                                                int count = 0
                                                model.hashBucket[it].each { 
                                                    count += it.certificates
                                                }
                                                count
                                            })
                                            closureColumn(header : trans("FEEDS"), preferredWidth : 20, type : Integer, read : {
                                                int count = 0
                                                model.hashBucket[it].each { 
                                                    if (it.feed)
                                                        count++
                                                }
                                                count
                                            })
                                            closureColumn(header : trans("CHAT_HOSTS"), preferredWidth : 20, type : Integer, read : {
                                                int count = 0
                                                model.hashBucket[it].each { 
                                                    if (it.chat)
                                                        count++
                                                }
                                                count
                                            })
                                            closureColumn(header : trans("COLLECTIONS"), preferredWidth : 20, type : Integer, read : {
                                                int count = 0
                                                model.hashBucket[it].each { UIResultEvent row ->
                                                    count += row.collections.size()
                                                }
                                                count
                                            })
                                        }
                                    }
                                }
                                panel (constraints : BorderLayout.SOUTH) {
                                    gridLayout(rows :1, cols : 3)
                                    panel {}
                                    panel {
                                        button(text : trans("DOWNLOAD"), enabled : bind {model.downloadActionEnabled}, downloadAction)
                                    }
                                    panel {
                                        gridBagLayout()
                                        label(text : trans("DOWNLOAD_SEQUENTIALLY"), constraints : gbc(gridx : 0, gridy : 0, weightx : 100, anchor : GridBagConstraints.LINE_END))
                                        sequentialDownloadCheckbox2 = checkBox( constraints : gbc(gridx: 1, gridy:0, weightx: 0, anchor : GridBagConstraints.LINE_END))
                                    }
                                }
                            }
                            panel {
                                borderLayout()
                                scrollPane(constraints : BorderLayout.CENTER) {
                                    sendersTable2 = table(id : "senders-table2", autoCreateRowSorter : true, rowHeight : rowHeight) {
                                        tableModel(list : model.senders2) {
                                            closureColumn(header : trans("SENDER"), preferredWidth : 350, type : String, read : {it.sender.getHumanReadableName()})
                                            closureColumn(header : trans("BROWSE"), preferredWidth : 20, type : Boolean, read : {it.browse})
                                            closureColumn(header : trans("FEED"), preferredWidth : 20, type: Boolean, read : {it.feed})
                                            closureColumn(header : trans("MESSAGES"), preferredWidth : 20, type: Boolean, read : {it.messages})
                                            closureColumn(header : trans("CHAT"), preferredWidth : 20, type : Boolean, read : {it.chat})
                                            closureColumn(header : trans("COMMENT"), preferredWidth : 20, type : Boolean, read : {it.comment != null})
                                            closureColumn(header : trans("CERTIFICATES"), preferredWidth : 20, type: Integer, read : {it.certificates})
                                            closureColumn(header : trans("COLLECTIONS"), preferredWidth : 20, type: Integer, read : {UIResultEvent row -> row.collections.size()})
                                            closureColumn(header : trans("TRUST_NOUN"), preferredWidth : 50, type : String, read : {
                                                trans(model.core.trustService.getLevel(it.sender.destination).name())
                                            })
                                        }
                                    }
                                } 
                                panel (constraints : BorderLayout.SOUTH) {
                                    gridLayout(rows : 1, cols : 5)
                                    panel (border : etchedBorder()) {
                                        gridLayout()
                                        button(text : trans("VIEW_COMMENT"), enabled : bind {model.viewCommentActionEnabled}, constraints : gbc(gridx : 0, gridy : 0), showCommentAction)
                                        button(text : trans("SUBSCRIBE"), enabled : bind {model.subscribeActionEnabled}, constraints : gbc(gridx : 1, gridy : 0), subscribeAction)
                                    }
                                    panel (border : etchedBorder()) {
                                        gridBagLayout()
                                        button(text : trans("VIEW_CERTIFICATES"), enabled : bind {model.viewCertificatesActionEnabled}, constraints : gbc(gridx : 0, gridy : 0), viewCertificatesAction)
                                        button(text : trans("VIEW_COLLECTIONS"), enabled : bind {model.viewCollectionsActionEnabled}, constraints : gbc(gridx : 1, gridy : 0), viewCollectionsAction)
                                    }
                                    panel (border : etchedBorder()) {
                                        gridBagLayout()
                                        button(text : trans("BROWSE_HOST"), enabled : bind {model.browseActionEnabled}, constraints : gbc(gridx : 0, gridy : 0), browseAction)
                                        button(text : trans("BROWSE_COLLECTIONS"), enabled : bind {model.browseCollectionsActionEnabled}, constraints : gbc(gridx : 1, gridy : 0), browseCollectionsAction)
                                    }
                                    panel (border : etchedBorder()) {
                                        gridBagLayout()
                                        button(text : trans("MESSAGE_VERB"), enabled : bind {model.messageActionEnabled}, constraints : gbc(gridx : 0, gridy :0), messageAction)
                                        button(text : trans("CHAT"), enabled : bind{model.chatActionEnabled}, constraints : gbc(gridx : 1, gridy : 0), chatAction)
                                    }
                                    panel (border : etchedBorder()) {
                                        button(text : trans("ADD_CONTACT"), enabled: bind {model.trustButtonsEnabled }, trustAction)
                                        button(text : trans("DISTRUST"), enabled : bind {model.trustButtonsEnabled}, distrustAction)
                                    }
                                }
                            }
                        }
                    }
                }
                panel (constraints : BorderLayout.SOUTH) {
                    label(text : trans("GROUP_BY"))
                    buttonGroup(id : "groupBy")
                    radioButton(text : trans("SENDER"), selected : bind  {!model.groupedByFile}, buttonGroup : groupBy, actionPerformed: showSenderGrouping)
                    radioButton(text : trans("FILE"), selected : bind {model.groupedByFile}, buttonGroup : groupBy, actionPerformed: showFileGrouping)
                }
            }

        this.pane.putClientProperty("mvc-group", mvcGroup)
        this.pane.putClientProperty("results-table",resultsTable)

        def selectionModel = resultsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener( {
            int[] rows = resultsTable.getSelectedRows()
            if (rows.length == 0) {
                model.downloadActionEnabled = false
                return
            }
            if (lastSortEvent != null) {
                for (int i = 0; i < rows.length; i ++) {
                    rows[i] = resultsTable.rowSorter.convertRowIndexToModel(rows[i])
                }
            }
            boolean downloadActionEnabled = true
            rows.each { 
                downloadActionEnabled &= mvcGroup.parentGroup.model.canDownload(model.results[it].infohash)
            }
            model.downloadActionEnabled = downloadActionEnabled
        })
    }

    void mvcGroupInit(Map<String, String> args) {
        searchTerms = args["search-terms"]
        parent = mvcGroup.parentGroup.view.builder.getVariable("result-tabs")
        parent.addTab(searchTerms, pane)
        int index = parent.indexOfComponent(pane)
        parent.setSelectedIndex(index)

        def tabPanel
        builder.with {
            tabPanel = panel {
                borderLayout()
                panel {
                    label(text : searchTerms, constraints : BorderLayout.CENTER)
                }
                button(icon : imageIcon("/close_tab.png"), preferredSize : [20,20], constraints : BorderLayout.EAST, // TODO: in osx is probably WEST
                    actionPerformed : closeTab )
            }
        }

        parent.setTabComponentAt(index, tabPanel)
        mvcGroup.parentGroup.view.showSearchWindow.call()

        
        // senders popup menu
        JPopupMenu popupMenu = new JPopupMenu()
        JMenuItem copyFullIDItem = new JMenuItem(trans("COPY_FULL_ID"))
        copyFullIDItem.addActionListener({mvcGroup.controller.copyFullID()})
        popupMenu.add(copyFullIDItem)
        
        def sendersMouseListener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    popupMenu.show(e.getComponent(), e.getX(), e.getY())
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    popupMenu.show(e.getComponent(), e.getX(), e.getY())
            }
        }
        
        // results table + tree mouse listener when grouped by sender
        def resultsMouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.button == MouseEvent.BUTTON3)
                    showPopupMenu(e)
                else if (e.button == MouseEvent.BUTTON1 && e.clickCount == 2)
                    controller.download()
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.button == MouseEvent.BUTTON3)
                    showPopupMenu(e)
            }
        }
        
        // results tree
        resultTree.addMouseListener(resultsMouseListener)
        resultTree.addTreeSelectionListener {
            model.downloadActionEnabled = false
            model.viewCommentActionEnabled = false
            model.viewCollectionsActionEnabled = false
            model.viewCertificatesActionEnabled = false
            TreePath [] selected = resultTree.selectionModel.getSelectionPaths()
            if (selected == null || selected.length == 0)
                return
            
            model.downloadActionEnabled = true
            UIResultEvent result = resultTree.singleResultSelected()
            if (result != null) {
                model.viewCommentActionEnabled = result.comment != null
                model.viewCollectionsActionEnabled = !result.collections.isEmpty()
                model.viewCertificatesActionEnabled = result.certificates > 0
            }
        }
        
        // results table1
        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        resultsTable.setDefaultRenderer(Integer.class,centerRenderer)

        resultsTable.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())


        resultsTable.rowSorter.addRowSorterListener({ evt -> lastSortEvent = evt})
        resultsTable.rowSorter.setSortsOnUpdates(true)


        resultsTable.addMouseListener(resultsMouseListener)
        
        resultsTable.getSelectionModel().addListSelectionListener({
            def result = getSelectedResult()
            if (result == null) {
                model.viewCommentActionEnabled = false
                model.viewCertificatesActionEnabled = false
                model.subscribeActionEnabled = false
                model.viewCollectionsActionEnabled = false
                return
            } else {
                model.viewCommentActionEnabled = result.comment != null
                model.viewCertificatesActionEnabled = result.certificates > 0
                model.viewCollectionsActionEnabled = !result.collections.isEmpty()
            }
        })
        
        // senders table
        sendersTable.addMouseListener(sendersMouseListener)
        sendersTable.setDefaultRenderer(Integer.class, centerRenderer)
        sendersTable.rowSorter.addRowSorterListener({evt -> lastSendersSortEvent = evt})
        sendersTable.rowSorter.setSortsOnUpdates(true)
        def selectionModel = sendersTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int row = selectedSenderRow()
            if (row < 0) {
                model.trustButtonsEnabled = false
                model.browseActionEnabled = false
                model.subscribeActionEnabled = false
                model.browseCollectionsActionEnabled = false
                model.chatActionEnabled = false
                model.messageActionEnabled = false
                return
            } else {
                Persona sender = model.senders[row]
                model.browseActionEnabled = model.sendersBucket[sender].first().browse
                model.browseCollectionsActionEnabled = model.sendersBucket[sender].first().browseCollections
                model.chatActionEnabled = model.sendersBucket[sender].first().chat
                model.messageActionEnabled = model.sendersBucket[sender].first().messages
                model.subscribeActionEnabled = model.sendersBucket[sender].first().feed &&
                    model.core.feedManager.getFeed(sender) == null
                model.trustButtonsEnabled = true
                
                model.results.clear()
                model.results.addAll(model.sendersBucket[sender])
                resultsTable.model.fireTableDataChanged()
                
                model.root.removeAllChildren()
                for(UIResultEvent event : model.sendersBucket[sender])
                    model.treeModel.addToTree(event)
                model.treeModel.nodeStructureChanged(model.root)
                TreeUtil.expand(resultTree)
            }
        })
        
        
        // results table 2
        resultsTable2.setDefaultRenderer(Integer.class,centerRenderer)
        resultsTable2.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())
        resultsTable2.rowSorter.addRowSorterListener({evt -> lastResults2SortEvent = evt})
        resultsTable2.rowSorter.setSortsOnUpdates(true)
        selectionModel = resultsTable2.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            UIResultEvent e = getSelectedResult()
            if (e == null) {
                model.trustButtonsEnabled = false
                model.browseActionEnabled = false
                model.browseCollectionsActionEnabled = false
                model.chatActionEnabled = false
                model.messageActionEnabled = false
                model.viewCertificatesActionEnabled = false
                model.viewCollectionsActionEnabled = false
                return
            }
            model.downloadActionEnabled = true
            def results = model.hashBucket[e.infohash]
            model.senders2.clear()
            model.senders2.addAll(results)
            int selectedRow = sendersTable2.getSelectedRow()
            sendersTable2.model.fireTableDataChanged()
            if (selectedRow < results.size())
                sendersTable2.selectionModel.setSelectionInterval(selectedRow,selectedRow)
        })
        
        resultsTable2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1 && e.button == MouseEvent.BUTTON1)
                    mvcGroup.controller.download()
            }
        })
        
        // TODO: add download right-click action
        
        // senders table 2
        sendersTable2.addMouseListener(sendersMouseListener)
        sendersTable2.setDefaultRenderer(Integer.class, centerRenderer)
        sendersTable2.rowSorter.addRowSorterListener({ evt -> lastSenders2SortEvent = evt})
        sendersTable2.rowSorter.setSortsOnUpdates(true)
        selectionModel = sendersTable2.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int row = selectedSenderRow()
            if (row < 0 || model.senders2[row] == null) {
                model.browseActionEnabled = false
                model.browseCollectionsActionEnabled = false
                model.chatActionEnabled = false
                model.messageActionEnabled = false
                model.subscribeActionEnabled = false
                model.viewCertificatesActionEnabled = false
                model.viewCollectionsActionEnabled = false
                model.trustButtonsEnabled = false
                model.viewCommentActionEnabled = false
                return
            }
            UIResultEvent e = model.senders2[row]
            model.browseActionEnabled = e.browse
            model.browseCollectionsActionEnabled = e.browseCollections
            model.chatActionEnabled = e.chat
            model.messageActionEnabled = e.messages
            model.subscribeActionEnabled = e.feed && model.core.feedManager.getFeed(e.getSender()) == null 
            model.trustButtonsEnabled = true
            model.viewCommentActionEnabled = e.comment != null
            model.viewCertificatesActionEnabled = e.certificates > 0
            model.viewCollectionsActionEnabled = !e.collections.isEmpty()
        })
       
        if (settings.groupByFile) {
            showFileGrouping.call()
        } else {
            showSenderGrouping.call()
            showTree.call()
        }
    }

    def closeTab = {
        int index = parent.indexOfTab(searchTerms)
        parent.removeTabAt(index)
        model.trustButtonsEnabled = false
        model.downloadActionEnabled = false
        mvcGroup.destroy()
    }

    def showPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu()
        boolean showMenu = false
        if (model.downloadActionEnabled) {
            JMenuItem download = new JMenuItem(trans("DOWNLOAD"))
            download.addActionListener({mvcGroup.controller.download()})
            menu.add(download)
            showMenu = true
        }
        
        boolean singleSelected
        if (model.treeVisible)
            singleSelected = resultTree.singleResultSelected() != null
        else
            singleSelected = resultsTable.getSelectedRows().length == 1
        if (singleSelected) {
            JMenuItem copyHashToClipboard = new JMenuItem(trans("COPY_HASH_TO_CLIPBOARD"))
            copyHashToClipboard.addActionListener({mvcGroup.view.copyHashToClipboard()})
            menu.add(copyHashToClipboard)
            JMenuItem copyNameToClipboard = new JMenuItem(trans("COPY_NAME_TO_CLIPBOARD"))
            copyNameToClipboard.addActionListener({mvcGroup.view.copyNameToClipboard()})
            menu.add(copyNameToClipboard)
            showMenu = true
            
            // show comment if any
            if (model.viewCommentActionEnabled) {
                JMenuItem showComment = new JMenuItem(trans("VIEW_COMMENT"))
                showComment.addActionListener({mvcGroup.controller.showComment()})
                menu.add(showComment)
            }
            
            // view certificates if any
            if (model.viewCertificatesActionEnabled) {
                JMenuItem viewCerts = new JMenuItem(trans("VIEW_CERTIFICATES"))
                viewCerts.addActionListener({mvcGroup.controller.viewCertificates()})
                menu.add(viewCerts)
            }
            
            // view collections if any
            if (model.viewCollectionsActionEnabled) {
                JMenuItem viewCols = new JMenuItem(trans("VIEW_COLLECTIONS"))
                viewCols.addActionListener({mvcGroup.controller.viewCollections()})
                menu.add(viewCols)
            }
        }
        if (showMenu)
            menu.show(e.getComponent(), e.getX(), e.getY())
    }
    
    UIResultEvent getSelectedResult() {
        if (model.groupedByFile) {
            int selectedRow = resultsTable2.getSelectedRow()
            if (selectedRow < 0)
                return null
            if (lastResults2SortEvent != null)
                selectedRow = resultsTable2.rowSorter.convertRowIndexToModel(selectedRow)
            InfoHash infohash = model.results2[selectedRow]
            
            Persona sender = selectedSender()
            if (sender == null) // really shouldn't happen
                return model.hashBucket[infohash].first()
            
            for (UIResultEvent candidate : model.hashBucket[infohash]) {
                if (candidate.sender == sender)
                    return candidate
            }
            
            // also shouldn't happen
            return model.hashBucket[infohash].first()
        } else {
            int[] selectedRows = resultsTable.getSelectedRows()
            if (selectedRows.length != 1)
                return null
            int selected = selectedRows[0]
            if (lastSortEvent != null)
                selected = resultsTable.rowSorter.convertRowIndexToModel(selected)
            return model.results[selected]
        }
    }
    
    List<UIResultEvent> selectedResults() {
        if (model.groupedByFile) {
            return [getSelectedResult()]
        } else {
            List<UIResultEvent> results = new ArrayList<>()
            if (model.treeVisible) {
                for (TreePath path : resultTree.getSelectionPaths())
                    TreeUtil.getLeafs(path.getLastPathComponent(), results)
            } else {
                int[] rows = view.resultsTable.getSelectedRows()
                if (rows.length == 0)
                    return null
                def sortEvt = view.lastSortEvent
                if (sortEvt != null) {
                    for (int i = 0; i < rows.length; i++) {
                        rows[i] = view.resultsTable.rowSorter.convertRowIndexToModel(rows[i])
                    }
                }
                rows.each { results.add(model.results[it]) }
            }
            return results
        }
    }
    
    List<ResultAndTargets> decorateResults(List<UIResultEvent> results) {
        List<ResultAndTargets> rv = new ArrayList<>()
        if (model.groupedByFile || !model.treeVisible) {
            // flat
            for(UIResultEvent event : results)
                rv << new ResultAndTargets(event, new File(event.name), null)
        } else
            rv.addAll(resultTree.decorateResults(results))
        rv
    }

    def copyHashToClipboard() {
        def result = getSelectedResult()
        if (result == null)
            return
        String hash = Base64.encode(result.infohash.getRoot())
        StringSelection selection = new StringSelection(hash)
        def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
        clipboard.setContents(selection, null)
    }
    
    def copyNameToClipboard() {
        def result = getSelectedResult()
        if (result == null)
            return
        StringSelection selection = new StringSelection(result.getName())
        def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
        clipboard.setContents(selection, null)
    }
    
    int selectedSenderRow() {
        if (model.groupedByFile) {
            int row = sendersTable2.getSelectedRow()
            if (row < 0)
                return row
            if (lastSenders2SortEvent != null) 
                row = sendersTable2.rowSorter.convertRowIndexToModel(row)
            return row
        } else {
            int row = sendersTable.getSelectedRow()
            if (row < 0)
                return -1
            if (lastSendersSortEvent != null)
                row = sendersTable.rowSorter.convertRowIndexToModel(row)
            return row
        }
    }
    
    Persona selectedSender() {
        int row = selectedSenderRow()
        if (row < 0)
            return null
        if (model.groupedByFile)
            return model.senders2[row]?.sender
        else
            return model.senders[row]
    }
    
    def showSenderGrouping = {
        model.groupedByFile = false  
        def cardsPanel = builder.getVariable("results-panel")
        cardsPanel.getLayout().show(cardsPanel, "grouped-by-sender")  
    }
    
    def showFileGrouping = {
        model.groupedByFile = true
        def cardsPanel = builder.getVariable("results-panel")
        cardsPanel.getLayout().show(cardsPanel, "grouped-by-file")
    }
    
    def showTree = {
        model.treeVisible = true
        resultsPanel.getLayout().show(resultsPanel, "tree")
    }
    
    def showTable = {
        model.treeVisible = false
        resultsPanel.getLayout().show(resultsPanel, "table")
    }
    
    boolean sequentialDownload() {
        if (model.groupedByFile)
            return sequentialDownloadCheckbox2.model.isSelected()
        else
            return sequentialDownloadCheckbox.model.isSelected()
    }
}