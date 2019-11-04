package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

import com.muwire.core.search.UIResultEvent

import java.awt.BorderLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class BrowseView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    BrowseModel model
    @MVCMember @Nonnull
    BrowseController controller

    def mainFrame
    def dialog
    def p
    def resultsTable
    def lastSortEvent
    void initUI() {
        int rowHeight = application.context.get("row-height")
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, model.host.getHumanReadableName(), true)
        dialog.setResizable(true)
        
        p = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.NORTH) {
                label(text: "Status:")
                label(text: bind {model.status.toString()})
                label(text : bind {model.totalResults == 0 ? "" : Math.round(model.resultCount * 100 / model.totalResults)+ "%"})
            }
            scrollPane (constraints : BorderLayout.CENTER){
                resultsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.results) {
                        closureColumn(header: "Name", preferredWidth: 350, type: String, read : {row -> row.name.replace('<','_')})
                        closureColumn(header: "Size", preferredWidth: 20, type: Long, read : {row -> row.size})
                        closureColumn(header: "Comments", preferredWidth: 20, type: Boolean, read : {row -> row.comment != null})
                    }
                }
            }
            panel (constraints : BorderLayout.SOUTH) {
                button(text : "Download", enabled : bind {model.downloadActionEnabled}, downloadAction)
                button(text : "View Comment", enabled : bind{model.viewCommentActionEnabled}, viewCommentAction)
                button(text : "Dismiss", dismissAction)
            }
        }
        
        
        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        resultsTable.setDefaultRenderer(Integer.class,centerRenderer)

        resultsTable.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())
        
        
        resultsTable.rowSorter.addRowSorterListener({evt -> lastSortEvent = evt})
        resultsTable.rowSorter.setSortsOnUpdates(true)
        def selectionModel = resultsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener({
            int[] rows = resultsTable.getSelectedRows()
            if (rows.length == 0) {
                model.downloadActionEnabled = false
                model.viewCommentActionEnabled = false
                return
            }
            
            if (lastSortEvent != null) {
                for (int i = 0; i < rows.length; i ++) {
                    rows[i] = resultsTable.rowSorter.convertRowIndexToModel(rows[i])
                }
            }
            
            boolean downloadActionEnabled = true
            if (rows.length == 1 && model.results[rows[0]].comment != null) 
                model.viewCommentActionEnabled = true
            else
                model.viewCommentActionEnabled = false
             
            rows.each {
                downloadActionEnabled &= mvcGroup.parentGroup.parentGroup.model.canDownload(model.results[it].infohash)
            }
            model.downloadActionEnabled = downloadActionEnabled
            
        })
        resultsTable.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e)
            }
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e)
            }
        })
    }
    
    private void showMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu()
        if (model.downloadActionEnabled) {
            JMenuItem download = new JMenuItem("Download")
            download.addActionListener({controller.download()})
            menu.add(download)
        }
        if (model.viewCommentActionEnabled) {
            JMenuItem viewComment = new JMenuItem("View Comment")
            viewComment.addActionListener({controller.viewComment()})
            menu.add(viewComment)
        }
        
        JMenuItem copyHash = new JMenuItem("Copy Hash To Clipboard")
        copyHash.addActionListener({
            List<UIResultEvent> results = selectedResults()
            def hash = ""
            for(Iterator<UIResultEvent> iter = results.iterator(); iter.hasNext();) {
                UIResultEvent result = iter.next()
                hash += Base64.encode(result.infohash.getRoot())
                if (iter.hasNext())
                    hash += "\n"
            }
            copyString(hash)
        })
        menu.add(copyHash)
        
        JMenuItem copyName = new JMenuItem("Copy Name To Clipboard")
        copyName.addActionListener({
            List<UIResultEvent> results = selectedResults()
            def name = ""
            for(Iterator<UIResultEvent> iter = results.iterator(); iter.hasNext();) {
                UIResultEvent result = iter.next()
                name += result.getName()
                if (iter.hasNext())
                    name += "\n"
            }
            copyString(name)
            
        })
        menu.add(copyName)
        
        menu.show(e.getComponent(), e.getX(), e.getY())
    }
    
    private static copyString(String s) {
        StringSelection selection = new StringSelection(s)
        def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
        clipboard.setContents(selection, null)
    }
    void mvcGroupInit(Map<String,String> args) {
        controller.register()
        
        dialog.getContentPane().add(p)
        dialog.setSize(700, 400)
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.addWindowListener( new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        dialog.show()
    }
    
    
    def selectedResults() {
        int [] rows = resultsTable.getSelectedRows()
        if (rows.length == 0)
            return null
        if (lastSortEvent != null) {
            for (int i = 0; i < rows.length; i ++) {
                rows[i] = resultsTable.rowSorter.convertRowIndexToModel(rows[i])
            }
        }
        
        List<UIResultEvent> rv = new ArrayList<>()
        for (Integer i : rows)
            rv << model.results[i]
        rv
    }
}