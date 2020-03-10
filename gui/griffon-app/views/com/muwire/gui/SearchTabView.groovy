package com.muwire.gui

import griffon.core.artifact.GriffonView
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
    
    UISettings settings

    def pane
    def parent
    def searchTerms
    def sendersTable, sendersTable2
    def lastSendersSortEvent
    def resultsTable, resultsTable2
    def lastSortEvent
    def lastResults2SortEvent, lastSenders2SortEvent
    def sequentialDownloadCheckbox
    def sequentialDownloadCheckbox2

    void initUI() {
        int rowHeight = application.context.get("row-height")
        builder.with {
            def resultsTable, resultsTable2
            def sendersTable, sendersTable2
            def sequentialDownloadCheckbox, sequentialDownloadCheckbox2
            def pane = panel {
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
                                            closureColumn(header : "Sender", preferredWidth : 500, type: String, read : {row -> row.getHumanReadableName()})
                                            closureColumn(header : "Results", preferredWidth : 20, type: Integer, read : {row -> model.sendersBucket[row].size()})
                                            closureColumn(header : "Browse", preferredWidth : 20, type: Boolean, read : {row -> model.sendersBucket[row].first().browse})
                                            closureColumn(header : "Feed", preferredWidth : 20, type : Boolean, read : {row -> model.sendersBucket[row].first().feed})
                                            closureColumn(header : "Chat", preferredWidth : 20, type : Boolean, read : {row -> model.sendersBucket[row].first().chat})
                                            closureColumn(header : "Trust", preferredWidth : 50, type: String, read : { row ->
                                                model.core.trustService.getLevel(row.destination).toString()
                                            })
                                        }
                                    }
                                }
                                panel(constraints : BorderLayout.SOUTH) {
                                    gridLayout(rows: 1, cols : 2)
                                    panel (border : etchedBorder()){
                                        button(text : "Browse Host", enabled : bind {model.browseActionEnabled}, browseAction)
                                        button(text : "Subscribe", enabled : bind {model.subscribeActionEnabled}, subscribeAction)
                                        button(text : "Chat", enabled : bind{model.chatActionEnabled}, chatAction)
                                    }
                                    panel (border : etchedBorder()){
                                        button(text : "Trust", enabled: bind {model.trustButtonsEnabled }, trustAction)
                                        button(text : "Neutral", enabled: bind {model.trustButtonsEnabled}, neutralAction)
                                        button(text : "Distrust", enabled : bind {model.trustButtonsEnabled}, distrustAction)
                                    }
                                }
                            }
                            panel {
                                borderLayout()
                                scrollPane (constraints : BorderLayout.CENTER) {
                                    resultsTable = table(id : "results-table", autoCreateRowSorter : true, rowHeight : rowHeight) {
                                        tableModel(list: model.results) {
                                            closureColumn(header: "Name", preferredWidth: 350, type: String, read : {row -> row.name.replace('<','_')})
                                            closureColumn(header: "Size", preferredWidth: 20, type: Long, read : {row -> row.size})
                                            closureColumn(header: "Direct Sources", preferredWidth: 50, type : Integer, read : { row -> model.hashBucket[row.infohash].size()})
                                            closureColumn(header: "Possible Sources", preferredWidth : 50, type : Integer, read : {row -> model.sourcesBucket[row.infohash].size()})
                                            closureColumn(header: "Comments", preferredWidth: 20, type: Boolean, read : {row -> row.comment != null})
                                            closureColumn(header: "Certificates", preferredWidth: 20, type: Integer, read : {row -> row.certificates})
                                        }
                                    }
                                }
                                panel(constraints : BorderLayout.SOUTH) {
                                    gridBagLayout()
                                    label(text : "", constraints : gbc(gridx : 0, gridy: 0, weightx : 100))
                                    button(text : "Download", enabled : bind {model.downloadActionEnabled}, constraints : gbc(gridx : 1, gridy:0), downloadAction)
                                    button(text : "View Comment", enabled : bind {model.viewCommentActionEnabled}, constraints : gbc(gridx:2, gridy:0),  showCommentAction)
                                    button(text : "View Certificates", enabled : bind {model.viewCertificatesActionEnabled}, constraints : gbc(gridx:3, gridy:0), viewCertificatesAction)
                                    label(text : "Download sequentially", constraints : gbc(gridx: 4, gridy: 0, weightx : 80, anchor : GridBagConstraints.LINE_END))
                                    sequentialDownloadCheckbox = checkBox(constraints : gbc(gridx : 5, gridy: 0, anchor : GridBagConstraints.LINE_END),
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
                                            closureColumn(header : "Name", preferredWidth : 350, type : String, read : {
                                                model.hashBucket[it].first().name.replace('<', '_')
                                            })
                                            closureColumn(header : "Size", preferredWidth : 20, type : Long, read : {
                                                model.hashBucket[it].first().size
                                            })
                                            closureColumn(header : "Direct Sources", preferredWidth : 20, type : Integer, read : {
                                                model.hashBucket[it].size()
                                            })
                                            closureColumn(header : "Possible Sources", preferredWidth : 20, type : Integer , read : {
                                                model.sourcesBucket[it].size()
                                            })
                                            closureColumn(header : "Comments", preferredWidth : 20, type : Integer, read : {
                                                int count = 0
                                                model.hashBucket[it].each { 
                                                    if (it.comment != null)
                                                        count++
                                                }
                                                count
                                            })
                                            closureColumn(header : "Certificates", preferredWidth : 20, type : Integer, read : {
                                                int count = 0
                                                model.hashBucket[it].each { 
                                                    count += it.certificates
                                                }
                                                count
                                            })
                                            closureColumn(header : "Feeds", preferredWidth : 20, type : Integer, read : {
                                                int count = 0
                                                model.hashBucket[it].each { 
                                                    if (it.feed)
                                                        count++
                                                }
                                                count
                                            })
                                            closureColumn(header : "Chat Hosts", preferredWidth : 20, type : Integer, read : {
                                                int count = 0
                                                model.hashBucket[it].each { 
                                                    if (it.chat)
                                                        count++
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
                                        button(text : "Download", enabled : bind {model.downloadActionEnabled}, downloadAction)
                                    }
                                    panel {
                                        gridBagLayout()
                                        label(text : "Download sequentially", constraints : gbc(gridx : 0, gridy : 0, weightx : 100, anchor : GridBagConstraints.LINE_END))
                                        sequentialDownloadCheckbox2 = checkBox( constraints : gbc(gridx: 1, gridy:0, weightx: 0, anchor : GridBagConstraints.LINE_END))
                                    }
                                }
                            }
                            panel {
                                borderLayout()
                                scrollPane(constraints : BorderLayout.CENTER) {
                                    sendersTable2 = table(id : "senders-table2", autoCreateRowSorter : true, rowHeight : rowHeight) {
                                        tableModel(list : model.senders2) {
                                            closureColumn(header : "Sender", preferredWidth : 350, type : String, read : {it.sender.getHumanReadableName()})
                                            closureColumn(header : "Browse", preferredWidth : 20, type : Boolean, read : {it.browse})
                                            closureColumn(header : "Feed", preferredWidth : 20, type: Boolean, read : {it.feed})
                                            closureColumn(header : "Chat", preferredWidth : 20, type : Boolean, read : {it.chat})
                                            closureColumn(header : "Comment", preferredWidth : 20, type : Boolean, read : {it.comment != null})
                                            closureColumn(header : "Certificates", preferredWidth : 20, type: Integer, read : {it.certificates})
                                            closureColumn(header : "Trust", preferredWidth : 50, type : String, read : {
                                                model.core.trustService.getLevel(it.sender.destination).toString()
                                            })
                                        }
                                    }
                                } 
                                panel (constraints : BorderLayout.SOUTH) {
                                    gridLayout(rows : 1, cols : 2)
                                    panel (border : etchedBorder()) {
                                        button(text : "Browse Host", enabled : bind {model.browseActionEnabled}, browseAction)
                                        button(text : "Subscribe", enabled : bind {model.subscribeActionEnabled}, subscribeAction)
                                        button(text : "Chat", enabled : bind{model.chatActionEnabled}, chatAction)
                                        button(text : "View Comment", enabled : bind {model.viewCommentActionEnabled}, showCommentAction)
                                        button(text : "View Certificates", enabled : bind {model.viewCertificatesActionEnabled}, viewCertificatesAction)
                                    }
                                    panel (border : etchedBorder()) {
                                        button(text : "Trust", enabled: bind {model.trustButtonsEnabled }, trustAction)
                                        button(text : "Neutral", enabled: bind {model.trustButtonsEnabled}, neutralAction)
                                        button(text : "Distrust", enabled : bind {model.trustButtonsEnabled}, distrustAction)
                                    }
                                }
                            }
                        }
                    }
                }
                panel (constraints : BorderLayout.SOUTH) {
                    label(text : "Group by")
                    buttonGroup(id : "groupBy")
                    radioButton(text : "Sender", selected : bind  {!model.groupedByFile}, buttonGroup : groupBy, actionPerformed: showSenderGrouping)
                    radioButton(text : "File", selected : bind {model.groupedByFile}, buttonGroup : groupBy, actionPerformed: showFileGrouping)
                }
            }

            this.pane = pane
            this.pane.putClientProperty("mvc-group", mvcGroup)
            this.pane.putClientProperty("results-table",resultsTable)

            this.resultsTable = resultsTable
            this.sendersTable = sendersTable
            this.resultsTable2 = resultsTable2
            this.sendersTable2 = sendersTable2
            this.sequentialDownloadCheckbox = sequentialDownloadCheckbox
            this.sequentialDownloadCheckbox2 = sequentialDownloadCheckbox2

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

        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        resultsTable.setDefaultRenderer(Integer.class,centerRenderer)

        resultsTable.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())


        resultsTable.rowSorter.addRowSorterListener({ evt -> lastSortEvent = evt})
        resultsTable.rowSorter.setSortsOnUpdates(true)


        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.button == MouseEvent.BUTTON3)
                    showPopupMenu(e)
                else if (e.button == MouseEvent.BUTTON1 && e.clickCount == 2)
                    mvcGroup.controller.download()
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.button == MouseEvent.BUTTON3)
                    showPopupMenu(e)
            }
        })
        
        resultsTable.getSelectionModel().addListSelectionListener({
            def result = getSelectedResult()
            if (result == null) {
                model.viewCommentActionEnabled = false
                model.viewCertificatesActionEnabled = false
                model.subscribeActionEnabled = false
                return
            } else {
                model.viewCommentActionEnabled = result.comment != null
                model.viewCertificatesActionEnabled = result.certificates > 0
            }
        })
        
        // senders table
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
                return
            } else {
                Persona sender = model.senders[row]
                model.browseActionEnabled = model.sendersBucket[sender].first().browse
                model.chatActionEnabled = model.sendersBucket[sender].first().chat
                model.subscribeActionEnabled = model.sendersBucket[sender].first().feed
                model.trustButtonsEnabled = true
                model.results.clear()
                model.results.addAll(model.sendersBucket[sender])
                resultsTable.model.fireTableDataChanged()
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
                model.chatActionEnabled = false
                model.viewCertificatesActionEnabled = false
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
        sendersTable2.setDefaultRenderer(Integer.class, centerRenderer)
        sendersTable2.rowSorter.addRowSorterListener({ evt -> lastSenders2SortEvent = evt})
        sendersTable2.rowSorter.setSortsOnUpdates(true)
        selectionModel = sendersTable2.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int row = selectedSenderRow()
            if (row < 0 || model.senders2[row] == null) {
                model.browseActionEnabled = false
                model.chatActionEnabled = false
                model.subscribeActionEnabled = false
                model.viewCertificatesActionEnabled = false
                model.trustButtonsEnabled = false
                model.viewCommentActionEnabled = false
                return
            }
            model.browseActionEnabled = model.senders2[row].browse
            model.chatActionEnabled = model.senders2[row].chat
            model.subscribeActionEnabled = model.senders2[row].feed
            model.trustButtonsEnabled = true
            model.viewCommentActionEnabled = model.senders2[row].comment != null
            model.viewCertificatesActionEnabled = model.senders2[row].certificates > 0
        })
       
        if (settings.groupByFile)
            showFileGrouping.call()
        else
            showSenderGrouping.call()
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
            JMenuItem download = new JMenuItem("Download")
            download.addActionListener({mvcGroup.controller.download()})
            menu.add(download)
            showMenu = true
        }
        if (resultsTable.getSelectedRows().length == 1) {
            JMenuItem copyHashToClipboard = new JMenuItem("Copy hash to clipboard")
            copyHashToClipboard.addActionListener({mvcGroup.view.copyHashToClipboard()})
            menu.add(copyHashToClipboard)
            JMenuItem copyNameToClipboard = new JMenuItem("Copy name to clipboard")
            copyNameToClipboard.addActionListener({mvcGroup.view.copyNameToClipboard()})
            menu.add(copyNameToClipboard)
            showMenu = true
            
            // show comment if any
            if (model.viewCommentActionEnabled) {
                JMenuItem showComment = new JMenuItem("View Comment")
                showComment.addActionListener({mvcGroup.controller.showComment()})
                menu.add(showComment)
            }
            
            // view certificates if any
            if (model.viewCertificatesActionEnabled) {
                JMenuItem viewCerts = new JMenuItem("View Certificates")
                viewCerts.addActionListener({mvcGroup.controller.viewCertificates()})
                menu.add(viewCerts)
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
    
    boolean sequentialDownload() {
        if (model.groupedByFile)
            return sequentialDownloadCheckbox2.model.isSelected()
        else
            return sequentialDownloadCheckbox.model.isSelected()
    }
}