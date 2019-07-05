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
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

import com.muwire.core.util.DataUtil

import java.awt.BorderLayout
import java.awt.Color
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
    def resultsTable
    def lastSortEvent

    void initUI() {
        builder.with {
            def resultsTable
            def pane = scrollPane {
                resultsTable = table(id : "results-table", autoCreateRowSorter : true) {
                    tableModel(list: model.results) {
                        closureColumn(header: "Name", preferredWidth: 350, type: String, read : {row -> row.name.replace('<','_')})
                        closureColumn(header: "Size", preferredWidth: 20, type: Long, read : {row -> row.size})
                        closureColumn(header: "Direct Sources", preferredWidth: 50, type : Integer, read : { row -> model.hashBucket[row.infohash].size()})
                        closureColumn(header: "Possible Sources", preferredWidth : 50, type : Integer, read : {row -> model.sourcesBucket[row.infohash].size()})
                        closureColumn(header: "Sender", preferredWidth: 170, type: String, read : {row -> row.sender.getHumanReadableName()})
                        closureColumn(header: "Trust", preferredWidth: 50, type: String, read : {row ->
                          model.core.trustService.getLevel(row.sender.destination).toString()
                        })
                    }
                }
            }

            this.pane = pane
            this.pane.putClientProperty("mvc-group", mvcGroup)
            this.pane.putClientProperty("results-table",resultsTable)

            this.resultsTable = resultsTable

            def selectionModel = resultsTable.getSelectionModel()
            selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            selectionModel.addListSelectionListener( {
                int row = resultsTable.getSelectedRow()
                if (row < 0)
                    return
                if (lastSortEvent != null)
                    row = resultsTable.rowSorter.convertRowIndexToModel(row)
                mvcGroup.parentGroup.model.trustButtonsEnabled = true
                mvcGroup.parentGroup.model.downloadActionEnabled = mvcGroup.parentGroup.model.canDownload(model.results[row].infohash)
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

        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        resultsTable.columnModel.getColumn(1).setCellRenderer(centerRenderer)
        resultsTable.setDefaultRenderer(Integer.class,centerRenderer)
        resultsTable.columnModel.getColumn(4).setCellRenderer(centerRenderer)

        resultsTable.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())


        resultsTable.rowSorter.addRowSorterListener({ evt -> lastSortEvent = evt})
        resultsTable.rowSorter.setSortsOnUpdates(true)


        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.button == MouseEvent.BUTTON3)
                    showPopupMenu(e)
                else if (e.button == MouseEvent.BUTTON1 && e.clickCount == 2)
                    mvcGroup.parentGroup.controller.download()
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.button == MouseEvent.BUTTON3)
                    showPopupMenu(e)
            }
        })
    }

    def closeTab = {
        int index = parent.indexOfTab(searchTerms)
        parent.removeTabAt(index)
        mvcGroup.parentGroup.model.trustButtonsEnabled = false
        mvcGroup.parentGroup.model.downloadActionEnabled = false
        mvcGroup.destroy()
    }

    def showPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu()
        if (mvcGroup.parentGroup.model.downloadActionEnabled) {
            JMenuItem download = new JMenuItem("Download")
            download.addActionListener({mvcGroup.parentGroup.controller.download()})
            menu.add(download)
        }
        JMenuItem copyHashToClipboard = new JMenuItem("Copy hash to clipboard")
        copyHashToClipboard.addActionListener({mvcGroup.view.copyHashToClipboard()})
        menu.add(copyHashToClipboard)
        menu.show(e.getComponent(), e.getX(), e.getY())
    }

    def copyHashToClipboard() {
        int selected = resultsTable.getSelectedRow()
        if (selected < 0)
            return
        if (lastSortEvent != null)
            selected = resultsTable.rowSorter.convertRowIndexToModel(selected)
        String hash = Base64.encode(model.results[selected].infohash.getRoot())
        StringSelection selection = new StringSelection(hash)
        def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
        clipboard.setContents(selection, null)
    }
}