package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class BrowseView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    BrowseModel model

    def mainFrame
    def dialog
    def p
    def resultsTable
    def lastSortEvent
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, model.host.getHumanReadableName(), true)
        dialog.setResizable(true)
        
        p = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.NORTH) {
                label(text: "Status:")
                label(text: bind {model.status.toString()})
            }
            scrollPane (constraints : BorderLayout.CENTER){
                resultsTable = table(autoCreateRowSorter : true) {
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
                downloadActionEnabled &= mvcGroup.parentGroup.model.canDownload(model.results[it].infohash)
            }
            model.downloadActionEnabled = downloadActionEnabled
        })
    }
    
    void mvcGroupInit(Map<String,String> args) {
        dialog.getContentPane().add(p)
        dialog.pack()
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.addWindowListener( new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        dialog.show()
    }
    
}