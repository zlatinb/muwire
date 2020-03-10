package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

import com.muwire.core.filecert.Certificate

import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class FetchCertificatesView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    FetchCertificatesModel model
    @MVCMember @Nonnull
    FetchCertificatesController controller

    def mainFrame
    def dialog
    def p
    def certsTable
    def lastSortEvent
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, model.name, true)
        dialog.setResizable(true)
        
        p = builder.panel {
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                label(text : "Status:")
                label(text : bind {model.status.toString()})
                label(text : bind {model.certificateCount == 0 ? "" : Math.round(model.certificateCount * 100 / model.totalCertificates)+"%"})
            }
            scrollPane(constraints : BorderLayout.CENTER) {
                certsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.certificates) {
                        closureColumn(header : "Issuer", preferredWidth : 200, type : String, read : {it.issuer.getHumanReadableName()})
                        closureColumn(header : "Trust Status", preferredWidth: 50, type : String, read : {controller.core.trustService.getLevel(it.issuer.destination)})
                        closureColumn(header : "Name", preferredWidth : 200, type: String, read : {it.name.name.toString()})
                        closureColumn(header : "Issued", preferredWidth : 100, type : String, read : {
                            def date = new Date(it.timestamp)
                            date.toString()
                        })
                        closureColumn(header : "Comments", preferredWidth: 20, type : Boolean, read :{it.comment != null})
                    }
                }
            }
            panel(constraints : BorderLayout.SOUTH) {
                button(text : "Import", enabled : bind {model.importActionEnabled}, importCertificatesAction)
                button(text : "Show Comment", enabled : bind {model.showCommentActionEnabled}, showCommentAction)
                button(text : "Dismiss", dismissAction)
            }
        }
        
        certsTable.rowSorter.addRowSorterListener({evt -> lastSortEvent = evt})
        certsTable.rowSorter.setSortsOnUpdates(true)
        
        def selectionModel = certsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener({
            int[] rows = certsTable.getSelectedRows()
            model.importActionEnabled = rows.length > 0
            
            if (rows.length == 1) {
                if (lastSortEvent != null)
                    rows[0] = certsTable.rowSorter.convertRowIndexToModel(rows[0])
                model.showCommentActionEnabled = model.certificates[rows[0]].comment != null
            } else
                model.showCommentActionEnabled = false
            
        })
        
        certsTable.addMouseListener(new MouseAdapter() {
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
        JMenuItem importItem = new JMenuItem("Import")
        importItem.addActionListener({controller.importCertificates()})
        menu.add(importItem)
        if (model.showCommentActionEnabled) {
            JMenuItem showComment = new JMenuItem("Show Comment")
            showComment.addActionListener({controller.showComment()})
            menu.add(showComment)
        }
        menu.show(e.getComponent(), e.getX(), e.getY())
    }

    def selectedCertificates() {
        int [] rows = certsTable.getSelectedRows()
        if (rows.length == 0)
            return null
        if (lastSortEvent != null) {
            for(int i = 0; i< rows.length; i++) {
                rows[i] = certsTable.rowSorter.convertRowIndexToModel(rows[i])
            }
        }
        
        List<Certificate> rv = new ArrayList<>()
        for (Integer i : rows)
            rv << model.certificates[i]
        rv
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
    
}