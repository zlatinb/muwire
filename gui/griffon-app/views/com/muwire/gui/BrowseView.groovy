package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans
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

    def parent
    def p
    def resultsTable
    def lastSortEvent
    def sequentialDownloadCheckbox
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        
        p = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.NORTH) {
                label(text: trans("STATUS") + ":")
                label(text: bind {trans(model.status.name())})
                label(text : bind {model.totalResults == 0 ? "" : Math.round(model.resultCount * 100 / model.totalResults)+ "%"})
            }
            scrollPane (constraints : BorderLayout.CENTER){
                resultsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.results) {
                        closureColumn(header: trans("NAME"), preferredWidth: 350, type: String, read : {row -> row.name.replace('<','_')})
                        closureColumn(header: trans("SIZE"), preferredWidth: 20, type: Long, read : {row -> row.size})
                        closureColumn(header: trans("COMMENTS"), preferredWidth: 20, type: Boolean, read : {row -> row.comment != null})
                        closureColumn(header: trans("CERTIFICATES"), preferredWidth: 20, type: Integer, read : {row -> row.certificates})
                    }
                }
            }
            panel (constraints : BorderLayout.SOUTH) {
                button(text : trans("DOWNLOAD"), enabled : bind {model.downloadActionEnabled}, downloadAction)
                button(text : trans("VIEW_COMMENT"), enabled : bind{model.viewCommentActionEnabled}, viewCommentAction)
                button(text : trans("VIEW_CERTIFICATES"), enabled : bind{model.viewCertificatesActionEnabled}, viewCertificatesAction)
                button(text : trans("CHAT"), enabled : bind {model.chatActionEnabled}, chatAction)
                label(text : trans("DOWNLOAD_SEQUENTIALLY"))
                sequentialDownloadCheckbox = checkBox()
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
                model.viewCertificatesActionEnabled = false
                return
            }
            
            if (lastSortEvent != null) {
                for (int i = 0; i < rows.length; i ++) {
                    rows[i] = resultsTable.rowSorter.convertRowIndexToModel(rows[i])
                }
            }
            
            boolean downloadActionEnabled = true
            
            model.viewCertificatesActionEnabled = (rows.length == 1 && model.results[rows[0]].certificates > 0)
            
            if (rows.length == 1 && model.results[rows[0]].comment != null) 
                model.viewCommentActionEnabled = true
            else
                model.viewCommentActionEnabled = false
             
            def mainFrameGroup = application.mvcGroupManager.getGroups()['MainFrame']
            rows.each {
                downloadActionEnabled &= mainFrameGroup.model.canDownload(model.results[it].infohash)
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
            JMenuItem download = new JMenuItem(trans("DOWNLOAD"))
            download.addActionListener({controller.download()})
            menu.add(download)
        }
        if (model.viewCommentActionEnabled) {
            JMenuItem viewComment = new JMenuItem(trans("VIEW_COMMENT"))
            viewComment.addActionListener({controller.viewComment()})
            menu.add(viewComment)
        }
        if (model.viewCertificatesActionEnabled) {
            JMenuItem viewCertificates = new JMenuItem(trans("VIEW_CERTIFICATES"))
            viewCertificates.addActionListener({controller.viewCertificates()})
            menu.add(viewCertificates)
        }
        
        JMenuItem copyHash = new JMenuItem(trans("COPY_HASH_TO_CLIPBOARD"))
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
        
        JMenuItem copyName = new JMenuItem(trans("COPY_NAME_TO_CLIPBOARD"))
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
        
        def mainFrameGroup = application.mvcGroupManager.findGroup("MainFrame")
        parent = mainFrameGroup.view.builder.getVariable("result-tabs")
        parent.addTab(model.host.getHumanReadableName(), p)
        
        int index = parent.indexOfComponent(p)
        parent.setSelectedIndex(index)
     
        def tabPanel
        builder.with {
            tabPanel = panel {
                borderLayout()
                panel {
                    label(text : model.host.getHumanReadableName(), constraints : BorderLayout.CENTER)
                }
                button(icon : imageIcon("/close_tab.png"), preferredSize : [20,20], constraints : BorderLayout.EAST, // TODO: in osx is probably WEST
                    actionPerformed : closeTab )
            }
        }

        parent.setTabComponentAt(index, tabPanel)
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
    
    def closeTab = {
        int index = parent.indexOfTab(model.host.getHumanReadableName())
        parent.removeTabAt(index)
        model.downloadActionEnabled = false
        mvcGroup.destroy()
    }
}