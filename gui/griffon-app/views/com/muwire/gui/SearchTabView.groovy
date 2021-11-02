package com.muwire.gui

import com.muwire.core.SharedFile
import griffon.core.artifact.GriffonView

import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.tree.TreePath
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
    
    UISettings settings

    def pane
    def parent
    def searchTerms
    JTable sendersTable
    def lastSendersSortEvent
    JTable resultsTable, resultsTable2
    private JPanel resultsPanel
    private JPanel detailsPanelBySender, detailsPanelByFile
    private ResultTree resultTree
            
    def lastSortEvent
    def lastResults2SortEvent
    
    def sequentialDownloadCheckbox
    def sequentialDownloadCheckbox2

    private Map<InfoHash, MVCGroup> resultDetails = [:]
    
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
                                            tree(id: "results-tree", rowHeight: rowHeight, resultTree)
                                        }
                                    }
                                }
                                panel(constraints : BorderLayout.SOUTH) {
                                    gridLayout(rows: 1, cols: 3)
                                    panel {
                                        buttonGroup(id: "viewType")
                                        radioButton(text: trans("TREE"), selected: true, buttonGroup: viewType, actionPerformed: showTree)
                                        radioButton(text: trans("TABLE"), selected: false, buttonGroup: viewType, actionPerformed: showTable)
                                    }
                                    panel {
                                        button(text : trans("DOWNLOAD"), enabled : bind {model.downloadActionEnabled}, constraints : gbc(gridx : 1, gridy:0), downloadAction)
                                        label(text : trans("DOWNLOAD_SEQUENTIALLY"), constraints : gbc(gridx: 5, gridy: 0, weightx : 80, anchor : GridBagConstraints.LINE_END),
                                            enabled: bind{model.downloadActionEnabled})
                                        sequentialDownloadCheckbox = checkBox(constraints : gbc(gridx : 6, gridy: 0, anchor : GridBagConstraints.LINE_END),
                                            selected : false, enabled : bind {model.downloadActionEnabled})
                                    }
                                    panel {
                                        button(text: trans("VIEW_DETAILS"), enabled: bind {model.viewDetailsActionEnabled}, viewDetailsAction)
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
                                        button(text: trans("DOWNLOAD"), enabled: bind { model.downloadActionEnabled }, downloadAction)
                                        label(text: trans("DOWNLOAD_SEQUENTIALLY"))
                                        sequentialDownloadCheckbox2 = checkBox()
                                    }
                                    panel(border: etchedBorder()) {
                                        def textField = new JTextField(15)
                                        textField.addActionListener({ controller.filter() })
                                        widget(id: "filter-field", textField)
                                        button(text: trans("FILTER"), filterAction)
                                        button(text: trans("CLEAR"), enabled: bind { model.clearFilterActionEnabled }, clearFilterAction)
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
                    radioButton(text : trans("SENDER"), selected : bind  {!model.groupedByFile}, buttonGroup : groupBy, actionPerformed: showSenderGrouping)
                    radioButton(text : trans("FILE"), selected : bind {model.groupedByFile}, buttonGroup : groupBy, actionPerformed: showFileGrouping)
                }
            }

        this.pane.putClientProperty("mvc-group", mvcGroup)
        this.pane.putClientProperty("results-table",resultsTable)

        pane.putClientProperty("focusListener", new FocusListener())
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
        resultTree.setSharedPredicate(model.core.fileManager::isShared)
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
                new ResultNameTableCellRenderer(model.core.fileManager::isShared, false))

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
            boolean downloadActionEnabled = true
            rows.each {
                downloadActionEnabled &= mvcGroup.parentGroup.model.canDownload(model.results[it].infohash)
            }
            model.downloadActionEnabled = downloadActionEnabled
        })
        
        // senders table
        sendersTable.addMouseListener(sendersMouseListener)
        sendersTable.setDefaultRenderer(Integer.class, centerRenderer)
        sendersTable.rowSorter.addRowSorterListener({evt -> lastSendersSortEvent = evt})
        sendersTable.rowSorter.setSortsOnUpdates(true)
        selectionModel = sendersTable.getSelectionModel()
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
        resultsTable2.setDefaultRenderer(UIResultEvent.class, 
                new ResultNameTableCellRenderer(model.core.fileManager::isShared, false))
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
                model.trustButtonsEnabled = false
                model.browseActionEnabled = false
                model.browseCollectionsActionEnabled = false
                model.chatActionEnabled = false
                model.messageActionEnabled = false
                
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
    
    private void showResultDetailsByFile(UIResultEvent event) {
        detailsPanelByFile.removeAll()
        InfoHash infoHash = event.infohash
        
        MVCGroup group = resultDetails[infoHash]
        if (group == null) {
            String mvcId = model.uuid + Base64.encode(infoHash.getRoot())
            
            List<UIResultEvent> allResults = new ArrayList<>(model.hashBucket[infoHash].getResults())
            
            def params = [:]
            params.core = model.core
            params.fileName = event.name
            params.infoHash = infoHash
            params.results = allResults
            
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
        int index = parent.indexOfTab(searchTerms)
        parent.removeTabAt(index)
        model.trustButtonsEnabled = false
        model.downloadActionEnabled = false
        resultDetails.values().each {it.destroy()}
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
        
        
        table = builder.getVariable("results-table2")
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

    private class FocusListener {
        void onFocus(boolean focus) {
            model.visible = focus
        }
    }
}