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

    def pane
    def parent
    def searchTerms
    def sendersTable
    def lastSendersSortEvent
    def resultsTable
    def lastSortEvent
    def sequentialDownloadCheckbox

    void initUI() {
        int rowHeight = application.context.get("row-height")
        builder.with {
            def resultsTable
            def sendersTable
            def sequentialDownloadCheckbox
            def pane = panel {
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
                            gridLayout(rows: 1, cols: 3)
                            panel()
                            panel {
                                button(text : "Download", enabled : bind {model.downloadActionEnabled}, downloadAction)
                                button(text : "View Comment", enabled : bind {model.viewCommentActionEnabled}, showCommentAction)
                            }
                            panel {
                                gridBagLayout()
                                panel (constraints : gbc(gridx : 0, gridy : 0, weightx : 100))
                                sequentialDownloadCheckbox = checkBox(constraints : gbc(gridx : 1, gridy: 0, weightx : 0),selected : false, enabled : bind {model.downloadActionEnabled})
                                label(constraints: gbc(gridx: 2, gridy: 0, weightx : 0),text : "Download sequentially")
                            }
                        }
                    }
                }
            }

            this.pane = pane
            this.pane.putClientProperty("mvc-group", mvcGroup)
            this.pane.putClientProperty("results-table",resultsTable)

            this.resultsTable = resultsTable
            this.sendersTable = sendersTable
            this.sequentialDownloadCheckbox = sequentialDownloadCheckbox

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
                return
            } else {
                model.viewCommentActionEnabled = result.comment != null
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
                return
            } else {
                Persona sender = model.senders[row]
                model.browseActionEnabled = model.sendersBucket[sender].first().browse
                model.trustButtonsEnabled = true
                model.results.clear()
                model.results.addAll(model.sendersBucket[sender])
                resultsTable.model.fireTableDataChanged()
            }
        })
                
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
        }
        if (showMenu)
            menu.show(e.getComponent(), e.getX(), e.getY())
    }
    
    private UIResultEvent getSelectedResult() {
        int[] selectedRows = resultsTable.getSelectedRows()
        if (selectedRows.length != 1)
            return null
        int selected = selectedRows[0]
        if (lastSortEvent != null)
            selected = resultsTable.rowSorter.convertRowIndexToModel(selected)
        model.results[selected]
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
        int row = sendersTable.getSelectedRow()
        if (row < 0)
            return -1
        if (lastSendersSortEvent != null)
            row = sendersTable.rowSorter.convertRowIndexToModel(row)
        row
    }
}