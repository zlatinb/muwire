package com.muwire.gui

import com.muwire.core.SharedFile
import com.muwire.core.trust.TrustLevel
import com.muwire.gui.SearchTabModel.SenderBucket
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.PersonaOrProfileCellRenderer
import com.muwire.gui.profile.PersonaOrProfileComparator
import com.muwire.gui.profile.ResultPOP
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonView
import net.i2p.data.Destination

import javax.inject.Inject
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.RowSorter
import javax.swing.table.DefaultTableModel
import javax.swing.tree.TreePath
import java.awt.Component
import java.awt.Point
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.util.stream.Collectors

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
    @Inject
    GriffonApplication application
    
    UISettings settings

    JComponent pane
    JTabbedPane parent
    String searchTerms
    JTable sendersTable
    def lastSendersSortEvent
    JTable resultsTable, resultsTable2
    private JPanel resultsPanel
    private JPanel detailsPanelBySender, detailsPanelByFile
    private ResultTree resultTree
            
    def lastSortEvent
    def lastResults2SortEvent
    
    // caches the last selected sender to avoid re-rendering of the results
    int lastSelectedSenderRow = Integer.MIN_VALUE
    
    def sequentialDownloadCheckbox
    def sequentialDownloadCheckbox2

    private Map<InfoHash, MVCGroup> resultDetails = [:]
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        int treeRowHeight = application.context.get("tree-row-height")
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
                                            closureColumn(header : trans("SENDER"), preferredWidth : 600, type: PersonaOrProfile, read : { SenderBucket row -> row})
                                            closureColumn(header : trans("RESULTS"), preferredWidth : 20, type: Integer, read : {SenderBucket row -> row.results.size()})
                                            closureColumn(header : trans("BROWSE"), preferredWidth : 20, type: Boolean, read : {SenderBucket row -> row.results[0].browse})
                                            closureColumn(header : trans("COLLECTIONS"), preferredWidth : 20, type: Boolean, read : {SenderBucket row -> row.results[0].browseCollections})
                                            closureColumn(header : trans("FEED"), preferredWidth : 20, type : Boolean, read : {SenderBucket row -> row.results[0].feed})
                                            closureColumn(header : trans("MESSAGES"), preferredWidth : 20, type : Boolean, read : {SenderBucket row -> row.results[0].messages})
                                            closureColumn(header : trans("CHAT"), preferredWidth : 20, type : Boolean, read : {SenderBucket row -> row.results[0].chat})
                                            closureColumn(header : trans("TRUST_STATUS"), preferredWidth : 10, type: TrustLevel, read : { SenderBucket row ->
                                                model.core.trustService.getLevel(row.sender.destination)
                                            })
                                        }
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
                                                    closureColumn(header: trans("NAME"), preferredWidth: 350, type: UIResultEvent, read: { it })
                                                    closureColumn(header: trans("SIZE"), preferredWidth: 20, type: Long, read: { row -> row.size })
                                                    closureColumn(header: trans("DIRECT_SOURCES"), preferredWidth: 50, type: Integer, read: { row -> model.hashBucket[row.infohash].sourceCount() })
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
                                            tree(id: "results-tree", rowHeight: treeRowHeight, resultTree)
                                        }
                                    }
                                }
                                panel(constraints : BorderLayout.SOUTH) {
                                    gridLayout(rows: 1, cols: 3)
                                    panel {
                                        buttonGroup(id: "viewType")
                                        radioButton(text: trans("TREE"), toolTipText: trans("TOOLTIP_RESULT_VIEW_TREE"), selected: true, buttonGroup: viewType, actionPerformed: showTree)
                                        radioButton(text: trans("TABLE"), toolTipText: trans("TOOLTIP_RESULT_VIEW_TABLE"), selected: false, buttonGroup: viewType, actionPerformed: showTable)
                                    }
                                    panel {
                                        button(text : trans("DOWNLOAD"), toolTipText: trans("TOOLTIP_DOWNLOAD_FILE"), enabled : bind {model.downloadActionEnabled}, constraints : gbc(gridx : 1, gridy:0), downloadAction)
                                        label(text : trans("DOWNLOAD_SEQUENTIALLY"), toolTipText: trans("TOOLTIP_DOWNLOAD_SEQUENTIALLY"), constraints : gbc(gridx: 5, gridy: 0, weightx : 80, anchor : GridBagConstraints.LINE_END),
                                            enabled: bind{model.downloadActionEnabled})
                                        sequentialDownloadCheckbox = checkBox(constraints : gbc(gridx : 6, gridy: 0, anchor : GridBagConstraints.LINE_END),
                                            selected : false, enabled : bind {model.downloadActionEnabled})
                                    }
                                    panel {
                                        button(text: trans("VIEW_DETAILS"), toolTipText: trans("TOOLTIP_VIEW_DETAILS_RESULT"), enabled: bind {model.viewDetailsActionEnabled}, viewDetailsAction)
                                    }
                                }
                            }
                        }
                    }
                    panel (constraints : "grouped-by-file") {
                        gridLayout(rows : 1, cols : 1)
                        splitPane(orientation: JSplitPane.VERTICAL_SPLIT) {
                            panel {
                                borderLayout()
                                scrollPane(constraints: BorderLayout.CENTER) {
                                    resultsTable2 = table(id: "results-table2", autoCreateRowSorter: true, rowHeight: rowHeight) {
                                        tableModel(list: model.results2) {
                                            closureColumn(header: trans("NAME"), preferredWidth: 350, type: UIResultEvent, read: { model.hashBucket[it].firstEvent() })
                                            closureColumn(header: trans("SIZE"), preferredWidth: 20, type: Long, read: {
                                                model.hashBucket[it].getSize()
                                            })
                                            closureColumn(header: trans("DIRECT_SOURCES"), preferredWidth: 20, type: Integer, read: {
                                                model.hashBucket[it].sourceCount()
                                            })
                                            closureColumn(header: trans("POSSIBLE_SOURCES"), preferredWidth: 20, type: Integer, read: {
                                                model.sourcesBucket[it].size()
                                            })
                                            closureColumn(header: trans("COMMENTS"), preferredWidth: 20, type: Integer, read: {
                                                model.hashBucket[it].commentCount()
                                            })
                                            closureColumn(header: trans("CERTIFICATES"), preferredWidth: 20, type: Integer, read: {
                                                model.hashBucket[it].certificateCount()
                                            })
                                            closureColumn(header: trans("FEEDS"), preferredWidth: 20, type: Integer, read: {
                                                model.hashBucket[it].feedCount()
                                            })
                                            closureColumn(header: trans("CHAT_HOSTS"), preferredWidth: 20, type: Integer, read: {
                                                model.hashBucket[it].chatCount()
                                            })
                                            closureColumn(header: trans("COLLECTIONS"), preferredWidth: 20, type: Integer, read: {
                                                model.hashBucket[it].collectionsCount()
                                            })
                                        }
                                    }
                                }
                                panel(constraints: BorderLayout.SOUTH) {
                                    gridLayout(rows: 1, cols: 2)
                                    panel(border: etchedBorder()) {
                                        button(text: trans("DOWNLOAD"), toolTipText: trans("TOOLTIP_DOWNLOAD_FILE"), enabled: bind { model.downloadActionEnabled }, downloadAction)
                                        label(text: trans("DOWNLOAD_SEQUENTIALLY"), toolTipText: trans("TOOLTIP_DOWNLOAD_SEQUENTIALLY"))
                                        sequentialDownloadCheckbox2 = checkBox()
                                    }
                                    panel(border: etchedBorder()) {
                                        def textField = new JTextField(15)
                                        textField.addActionListener({ controller.filter() })
                                        widget(id: "filter-field", textField)
                                        button(text: trans("FILTER"), toolTipText: trans("TOOLTIP_FILTER_RESULTS"), filterAction)
                                        button(text: trans("CLEAR"), toolTipText: trans("TOOLTIP_FILTER_CLEAR"), enabled: bind { model.clearFilterActionEnabled }, clearFilterAction)
                                    }
                                }
                            }
                            detailsPanelByFile = panel {
                                gridLayout(rows: 1, cols: 1)
                            }
                        }
                    }
                }
                panel (constraints : BorderLayout.SOUTH) {
                    label(text : trans("GROUP_BY"))
                    buttonGroup(id : "groupBy")
                    radioButton(text : trans("SENDER"), toolTipText: trans("TOOLTIP_GROUP_SENDER"), selected : bind  {!model.groupedByFile}, 
                            buttonGroup : groupBy, actionPerformed: showSenderGrouping)
                    radioButton(text : trans("FILE"), toolTipText: trans("TOOLTIP_GROUP_FILE"), selected : bind {model.groupedByFile}, 
                            buttonGroup : groupBy, actionPerformed: showFileGrouping)
                }
            }

        this.pane.putClientProperty("mvc-group", mvcGroup)
        this.pane.putClientProperty("results-table",resultsTable)

        pane.putClientProperty("focusListener", new FocusListener())
        
        pane.with {
            registerKeyboardAction(closeTab,
                    KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK),
                    WHEN_IN_FOCUSED_WINDOW)
            registerKeyboardAction(repeatSearch,
                    KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK),
                    WHEN_IN_FOCUSED_WINDOW)
        }
        
        Action downloadAction = new AbstractAction() {
            @Override
            void actionPerformed(ActionEvent e) {
                controller.download()
            }
        }
        ["results-tree", "results-table", "results-table2"].each {
            JComponent resultsComponent = builder.getVariable(it)
            resultsComponent.registerKeyboardAction(downloadAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_FOCUSED)
        }
    }

    void mvcGroupInit(Map<String, String> args) {
        searchTerms = args["search-terms"]
        parent = mvcGroup.parentGroup.view.builder.getVariable("result-tabs")
        if (model.tab == null) {
            parent.addTab(searchTerms, pane)
            model.tab = parent.indexOfComponent(pane)
        } else 
            parent.insertTab(searchTerms, null, pane,null, model.tab)
        parent.setSelectedIndex(model.tab)

        JPanel tabPanel = builder.panel {
            borderLayout()
            panel {
                label(text: searchTerms, constraints: BorderLayout.CENTER)
            }
            panel(constraints: BorderLayout.EAST) {
                button(icon: imageIcon("/restart.png"), preferredSize: [20, 20],
                        toolTipText: trans("TOOLTIP_REPEAT_SEARCH"),
                        actionPerformed: repeatSearch)
                button(icon: imageIcon("/close_tab.png"), preferredSize: [20, 20],
                        toolTipText: trans("TOOLTIP_CLOSE_TAB"),
                        actionPerformed: closeTab)
            }
        }

        parent.setTabComponentAt(model.tab, tabPanel)
        mvcGroup.parentGroup.view.showSearchWindow.call()

        
        // senders popup menu
        def sendersMouseListener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    showSendersPopupMenu(e)
            }
            public void mouseReleased(MouseEvent e) {
                if(e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    showSendersPopupMenu(e)
            }
        }
        
        // results table + tree mouse listener when grouped by sender
        def resultsMouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.button == MouseEvent.BUTTON3)
                    showPopupMenu(e)
                else if (e.button == MouseEvent.BUTTON1 && e.clickCount == 2) {
                    if (model.groupedByFile || !model.treeVisible)
                        controller.download()
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.button == MouseEvent.BUTTON3)
                    showPopupMenu(e)
            }
        }
        
        // results tree
        resultTree.setSharedPredicate(model.core.fileManager::isShared, model.core.downloadManager::isDownloading)
        resultTree.addMouseListener(resultsMouseListener)
        resultTree.addTreeSelectionListener {
            model.downloadActionEnabled = false
            model.viewDetailsActionEnabled = false
            TreePath [] selected = resultTree.selectionModel.getSelectionPaths()
            if (selected == null || selected.length == 0)
                return
            
            model.downloadActionEnabled = true
            UIResultEvent result = resultTree.singleResultSelected()
            if (result != null) {
                model.viewDetailsActionEnabled = true
            }
        }
        
        // results table1
        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        resultsTable.setDefaultRenderer(Integer.class,centerRenderer)
        resultsTable.setDefaultRenderer(UIResultEvent.class,
                new ResultNameTableCellRenderer(model.core.fileManager::isShared,
                        model.core.downloadManager::isDownloading,
                        false))

        resultsTable.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())


        resultsTable.rowSorter.addRowSorterListener({ evt -> lastSortEvent = evt})
        resultsTable.rowSorter.setSortsOnUpdates(true)
        resultsTable.rowSorter.setComparator(0, new UIResultEventComparator(false))

        resultsTable.addMouseListener(resultsMouseListener)


        def selectionModel = resultsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener( {
            int[] rows = resultsTable.getSelectedRows()
            if (rows.length == 0) {
                model.downloadActionEnabled = false
                model.viewDetailsActionEnabled = false
                return
            }
            model.viewDetailsActionEnabled = rows.length == 1

            if (lastSortEvent != null) {
                for (int i = 0; i < rows.length; i ++) {
                    rows[i] = resultsTable.rowSorter.convertRowIndexToModel(rows[i])
                }
            }
            model.downloadActionEnabled = true
        })
        
        // senders table
        def popRenderer = new PersonaOrProfileCellRenderer(application.context.get("ui-settings"))
        def popComparator = new PersonaOrProfileComparator()
        sendersTable.addMouseListener(sendersMouseListener)
        sendersTable.setDefaultRenderer(TrustLevel.class, new TrustCellRenderer())
        sendersTable.setDefaultRenderer(Integer.class, centerRenderer)
        sendersTable.setDefaultRenderer(PersonaOrProfile.class, popRenderer)
        sendersTable.rowSorter.setComparator(0, popComparator)
        sendersTable.rowSorter.addRowSorterListener({evt -> lastSendersSortEvent = evt})
        sendersTable.rowSorter.setSortsOnUpdates(true)
        selectionModel = sendersTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int row = selectedSenderRow()
            if (row == lastSelectedSenderRow)
                return
            lastSelectedSenderRow = row
            if (row < 0) {
                model.viewProfileActionEnabled = false
                model.browseActionEnabled = false
                model.subscribeActionEnabled = false
                model.browseCollectionsActionEnabled = false
                model.chatActionEnabled = false
                model.messageActionEnabled = false
                return
            } else {
                SenderBucket bucket = model.senders[row]
                Persona sender = bucket.sender
                model.browseActionEnabled = bucket.results[0].browse
                model.browseCollectionsActionEnabled = bucket.results[0].browseCollections
                model.chatActionEnabled = bucket.results[0].chat
                model.messageActionEnabled = bucket.results[0].messages
                model.subscribeActionEnabled = bucket.results[0].feed &&
                    model.core.feedManager.getFeed(sender) == null
                model.viewProfileActionEnabled = true
                
                model.results.clear()
                model.results.addAll(bucket.results)
                resultsTable.model.fireTableDataChanged()
                
                model.root.removeAllChildren()
                for(UIResultEvent event : bucket.results)
                    model.treeModel.addToTree(event)
                model.treeModel.nodeStructureChanged(model.root)
                TreeUtil.expand(resultTree)
            }
        })
        
        
        // results table 2
        resultsTable2.setDefaultRenderer(Integer.class,centerRenderer)
        resultsTable2.setDefaultRenderer(UIResultEvent.class, 
                new ResultNameTableCellRenderer(model.core.fileManager::isShared, 
                        model.core.downloadManager::isDownloading,
                        false))
        resultsTable2.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())
        resultsTable2.rowSorter.addRowSorterListener({evt -> lastResults2SortEvent = evt})
        resultsTable2.rowSorter.setSortsOnUpdates(true)
        resultsTable2.rowSorter.setComparator(0, new UIResultEventComparator(false))
        selectionModel = resultsTable2.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener({
            detailsPanelByFile.removeAll()
            def p = new JPanel()
            p.add(new JLabel(trans("SELECT_SINGLE_RESULT")))
            detailsPanelByFile.add(p)
            detailsPanelByFile.updateUI()
            
            List<UIResultEvent> selectedResults = selectedResults()
            if (selectedResults.isEmpty()) {
                model.downloadActionEnabled = false
                model.browseActionEnabled = false
                model.browseCollectionsActionEnabled = false
                model.chatActionEnabled = false
                model.messageActionEnabled = false
                model.viewProfileActionEnabled = false
                
                return
            }
            
            model.downloadActionEnabled = true
            if (selectedResults.size() == 1) {
                showResultDetailsByFile(selectedResults.first())
            }
        })
        
        resultsTable2.addMouseListener(resultsMouseListener)
        
        
       
        showTree.call()
        if (settings.groupByFile) {
            showFileGrouping.call()
        } else {
            showSenderGrouping.call()
        }
    }
    
    private void showSendersPopupMenu(MouseEvent event) {
        if (!RightClickSupport.processRightClick(event))
            return

        JPopupMenu popupMenu = new JPopupMenu()
        JMenuItem viewProfileItem = new JMenuItem(trans("VIEW_PROFILE"))
        viewProfileItem.setToolTipText(trans("TOOLTIP_VIEW_PROFILE"))
        viewProfileItem.addActionListener({mvcGroup.controller.viewProfile()})
        popupMenu.add(viewProfileItem)
        
        if (model.browseActionEnabled || model.browseCollectionsActionEnabled) {
            popupMenu.addSeparator()
            if (model.browseActionEnabled) {
                JMenuItem browseItem = new JMenuItem(trans("BROWSE_HOST"))
                browseItem.setToolTipText(trans("TOOLTIP_BROWSE_FILES_SENDER"))
                browseItem.addActionListener({ controller.browse() })
                popupMenu.add(browseItem)
            }
            if (model.browseCollectionsActionEnabled) {
                JMenuItem browseItem = new JMenuItem(trans("BROWSE_COLLECTIONS"))
                browseItem.setToolTipText(trans("TOOLTIP_BROWSE_COLLECTIONS_SENDER"))
                browseItem.addActionListener({ controller.browseCollections() })
                popupMenu.add(browseItem)
            }
        }
        if (model.subscribeActionEnabled) {
            popupMenu.addSeparator()
            JMenuItem subscribeItem = new JMenuItem(trans("SUBSCRIBE"))
            subscribeItem.setToolTipText(trans("TOOLTIP_SUBSCRIBE_FILE_FEED"))
            subscribeItem.addActionListener({controller.subscribe()})
            popupMenu.add(subscribeItem)
        }
        if (model.messageActionEnabled || model.chatActionEnabled) {
            popupMenu.addSeparator()
            if (model.messageActionEnabled) {
                JMenuItem messageItem = new JMenuItem(trans("MESSAGE_VERB"))
                messageItem.setToolTipText(trans("TOOLTIP_MESSAGE_SENDER"))
                messageItem.addActionListener({controller.message()})
                popupMenu.add(messageItem)
            }
            if (model.chatActionEnabled) {
                JMenuItem chatItem = new JMenuItem(trans("CHAT"))
                chatItem.setToolTipText(trans("TOOLTIP_CHAT_SENDER"))
                chatItem.addActionListener({controller.chat()})
                popupMenu.add(chatItem)
            }
        }
        popupMenu.show(event.getComponent(), event.getX(), event.getY())
    }
    
    private void showResultDetailsByFile(UIResultEvent event) {
        detailsPanelByFile.removeAll()
        InfoHash infoHash = event.infohash
        
        MVCGroup group = resultDetails[infoHash]
        if (group == null) {
            String mvcId = model.uuid + Base64.encode(infoHash.getRoot())
            
            List<ResultPOP> allResults = model.hashBucket[infoHash].getResults().collect{new ResultPOP(it)}
            
            def params = [:]
            params.core = model.core
            params.fileName = event.name
            params.infoHash = infoHash
            params.results = allResults
            params.uuid = model.uuid
            
            group = mvcGroup.createMVCGroup("result-details-tabs", mvcId, params)
            resultDetails[infoHash] = group
        }
        
        group.view.buildTabs()
        detailsPanelByFile.add(group.view.p, null)
        detailsPanelByFile.updateUI()
    }

    void addResultToDetailMaps(UIResultEvent event) {
        resultDetails[event.infohash]?.model?.addResult(event)
    }

    def closeTab = {
        model.tab = parent.indexOfComponent(pane)
        parent.removeTabAt(model.tab)
        model.viewProfileActionEnabled = false
        model.downloadActionEnabled = false
        resultDetails.values().each {it.destroy()}
        mvcGroup.destroy()
    }
    
    def repeatSearch = {
        int tab = parent.indexOfComponent(pane)
        Boolean regex = model.regex
        def parentGroup = mvcGroup.parentGroup
        closeTab.call()
        parentGroup.controller.repeatSearch(searchTerms, tab, regex)
    }

    def showPopupMenu(MouseEvent e) {
        
        if (!RightClickSupport.processRightClick(e))
            return
        
        JPopupMenu menu = new JPopupMenu()
        boolean showMenu = false
        if (model.downloadActionEnabled) {
            JMenuItem download = new JMenuItem(trans("DOWNLOAD"))
            download.addActionListener({mvcGroup.controller.download()})
            menu.add(download)
            showMenu = true
        }
        
        boolean singleSelected
        if (model.groupedByFile)
            singleSelected = resultsTable2.getSelectedRows().length == 1
        else {
            if (model.treeVisible)
                singleSelected = resultTree.singleResultSelected() != null
            else
                singleSelected = resultsTable.getSelectedRows().length == 1
        }
        if (singleSelected) {
            JMenuItem copyHashToClipboard = new JMenuItem(trans("COPY_HASH_TO_CLIPBOARD"))
            copyHashToClipboard.addActionListener({mvcGroup.view.copyHashToClipboard()})
            menu.add(copyHashToClipboard)
            JMenuItem copyNameToClipboard = new JMenuItem(trans("COPY_NAME_TO_CLIPBOARD"))
            copyNameToClipboard.addActionListener({mvcGroup.view.copyNameToClipboard()})
            menu.add(copyNameToClipboard)
            showMenu = true
            
            if (!model.groupedByFile) {
                JMenuItem showDetails = new JMenuItem(trans("VIEW_DETAILS"))
                showDetails.addActionListener({ controller.viewDetails() })
                menu.add(showDetails)
            }
        }
        if (showMenu)
            menu.show(e.getComponent(), e.getX(), e.getY())
    }
    
    private UIResultEvent getSelectedResult() {
        int selectedRow = resultsTable2.getSelectedRow()
        if (selectedRow < 0)
            return null
        if (lastResults2SortEvent != null)
            selectedRow = resultsTable2.rowSorter.convertRowIndexToModel(selectedRow)
        InfoHash infohash = model.results2[selectedRow]
        model.hashBucket[infohash].firstEvent()
    }
    
    List<UIResultEvent> selectedResults() {
        if (model.groupedByFile) {
            int[] selectedRows = resultsTable2.getSelectedRows()
            if (selectedRows == null || selectedRows.length == 0)
                return Collections.emptyList()
            if (selectedRows.length == 1)
                return [getSelectedResult()]
            
            if (lastResults2SortEvent != null) {
                for (int i = 0; i < selectedRows.length; i++)
                    selectedRows[i] = resultsTable2.rowSorter.convertRowIndexToModel(selectedRows[i])
            }
            
            List<UIResultEvent> rv = []
            for (int row : selectedRows) {
                def ih = model.results2[row]
                rv.addAll model.hashBucket[ih].getResults()
            }
            return rv
        } else {
            List<UIResultEvent> results = new ArrayList<>()
            if (model.treeVisible) {
                for (TreePath path : resultTree.getSelectionPaths())
                    TreeUtil.getLeafs(path.getLastPathComponent(), results)
            } else {
                int[] rows = resultsTable.getSelectedRows()
                if (rows.length == 0)
                    return null
                def sortEvt = lastSortEvent
                if (sortEvt != null) {
                    for (int i = 0; i < rows.length; i++) {
                        rows[i] = resultsTable.rowSorter.convertRowIndexToModel(rows[i])
                    }
                }
                rows.each { results.add(model.results[it]) }
            }
            return results
        }
    }
    
    void clearSelections() {
        resultsTable2.clearSelection()
        resultsTable.clearSelection()
        sendersTable.clearSelection()
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
        def results = selectedResults()
        if (results.isEmpty())
            return
        
        String joined = results.stream().
                map({Base64.encode(it.infohash.getRoot())}).
                collect(Collectors.joining("\n"))
        
        CopyPasteSupport.copyToClipboard(joined)
    }
    
    def copyNameToClipboard() {
        def results = selectedResults()
        if (results.isEmpty())
            return
        
        String joined = results.stream().
                map(UIResultEvent::getName).
                collect(Collectors.joining("\n"))

        CopyPasteSupport.copyToClipboard(joined)
    }
    
    int selectedSenderRow() {
        if (model.groupedByFile) {
            return -1
        } else {
            int row = sendersTable.getSelectedRow()
            if (row < 0)
                return -1
            if (lastSendersSortEvent != null)
                row = sendersTable.rowSorter.convertRowIndexToModel(row)
            return row
        }
    }
    
    PersonaOrProfile selectedSender() {
        int row = selectedSenderRow()
        if (row < 0)
            return null
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
    
    void addPendingResults() {
        JTable table = builder.getVariable("senders-table")
        final int selectedRow = table.getSelectedRow()
        int selectedRowToModel = -1
        if (selectedRow >= 0)
            selectedRowToModel = table.rowSorter.convertRowIndexToModel(selectedRow)

        int newRowsStart = -1
        int newRowsEnd = -1
        for (int row = 0; row < model.senders.size(); row++) {
            SenderBucket sb = model.senders[row]
            List<UIResultEvent> pending = sb.getPendingResults()
            if (pending.isEmpty())
                continue
            if (pending.size() == sb.results.size()) {
                if (newRowsStart == -1)
                    newRowsStart = row
                newRowsEnd = row
            } else {
                table.model.fireTableRowsUpdated(row, row)
            }
            if (row == selectedRow) {
                lastSelectedSenderRow = Integer.MIN_VALUE
                displayPendingResults(pending)
            }
        }

        if (newRowsStart >= 0 && newRowsEnd >= 0) {
            table.model.fireTableRowsInserted(newRowsStart, newRowsEnd)
        } else {
            boolean shouldSort = false
            for (RowSorter.SortKey key : table.rowSorter.getSortKeys()) {
                if (key.column == 1) {
                    shouldSort = true
                    break
                }
            }

            if (shouldSort) {
                table.model.fireTableDataChanged()
            }
        }
        
        if (selectedRowToModel >= 0) {
            int selectedRowToView = table.rowSorter.convertRowIndexToView(selectedRowToModel)
            table.selectionModel.setSelectionInterval(selectedRowToView, selectedRowToView)
        }
    }
    
    private void displayPendingResults(List<UIResultEvent> pending) {
        int rowCount = model.results.size()
        model.results.addAll(pending)
        resultsTable.model.fireTableRowsInserted(rowCount - 1, model.results.size() - 1)
        if (!resultsTable.rowSorter.getSortKeys().isEmpty())
            resultsTable.model.fireTableDataChanged()
        
        for (UIResultEvent event : pending) 
            model.treeModel.addToTree(event)
        model.treeModel.nodeStructureChanged(model.root)
        // TODO: decide whether to expand
    }
    
    void refreshResults() {
        JTable table = builder.getVariable("senders-table")
        int selectedRow = table.getSelectedRow()
        if (selectedRow >= 0)
            selectedRow = table.rowSorter.convertRowIndexToModel(selectedRow)
        table.model.fireTableDataChanged()
        if (selectedRow >= 0) {
            selectedRow = table.rowSorter.convertRowIndexToView(selectedRow)
            table.selectionModel.setSelectionInterval(selectedRow, selectedRow)
        }
        
        updateResultsTable2()
    }
    
    void updateResultsTable2() {
        JTable table = builder.getVariable("results-table2")
        int [] selectedRows = table.getSelectedRows()
        for (int i = 0; i < selectedRows.length; i++)
            selectedRows[i] = table.rowSorter.convertRowIndexToModel(selectedRows[i])
        table.model.fireTableDataChanged()
        for (int i = 0; i < selectedRows.length; i++) {
            if (selectedRows[i] >= table.getRowCount())
                selectedRows[i] = -1
            else
                selectedRows[i] = table.rowSorter.convertRowIndexToView(selectedRows[i])
        }
        for (int row : selectedRows) {
            if (row >= 0)
                table.selectionModel.addSelectionInterval(row, row)
        }
    }
    
    void onTrustChanged(Persona persona) {
        // 1. check Senders table in group-by-sender mode.
        // there should be exactly 1 entry if at all.
        if (!model.sendersBucket.containsKey(persona))
            return
        int index = model.sendersBucket[persona].rowIdx
        
        // 2. it exists in the senders table, update the row
        JTable table = builder.getVariable("senders-table")
        try {
            table.model.fireTableRowsUpdated(index, index)
        } catch (IndexOutOfBoundsException strange) {} // TODO: investigate
        
        // 3. if the senders table was sorted by trust status, re-sort
        List<RowSorter.SortKey> keys = table.rowSorter.getSortKeys()
        if (!keys.isEmpty()) {
            boolean shouldSort = false
            for (RowSorter.SortKey key : keys) {
                if (key.column == 7) {
                    shouldSort = true
                    break
                }
            }
            if (shouldSort)
                table.rowSorter.allRowsChanged()
        }
        
        // 4. for the group-by-file view, only update if a single result is selected
        table = builder.getVariable("results-table2")
        int[] selectedRows = table.getSelectedRows()
        if (selectedRows.length != 1)
            return
        
        // cheat - it's too expensive to figure out if the result was relevant
        detailsPanelByFile.updateUI()
    }
    
    void updateUIs() {
        JTable table = builder.getVariable("results-table")
        table.updateUI()
        resultTree.updateUI()
    }

    private class FocusListener {
        void onFocus(boolean focus) {
            model.visible = focus
        }
    }
}