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
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, model.result.name, true)
        dialog.setResizable(true)
        
        p = builder.panel {
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                label(text : "Status:")
                label(text : bind {model.status.toString()})
                label(text : bind {model.certificateCount == 0 ? "" : Math.round(model.certificateCount * 100 / model.totalCertificates)+"%"})
            }
            scrollPane(constraints : BorderLayout.CENTER) {
                certsTable = table(autoCreateRowSorter : true) {
                    tableModel(list : model.certificates) {
                        closureColumn(header : "Issuer", preferredWidth : 200, type : String, read : {it.issuer.getHumanReadableName()})
                        closureColumn(header : "Name", preferredWidth : 200, type: String, read : {it.name.toString()})
                        closureColumn(header : "Issued", preferredWidth : 100, type : String, read : {
                            def date = new Date(it.timestamp)
                            date.toString()
                        })
                    }
                }
            }
            panel(constraints : BorderLayout.SOUTH) {
                button(text : "Import", enabled : bind {model.importActionEnabled}, importCertificatesAction)
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
        menu.showing(e.getComponent(), e.getX(), e.getY())
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
    
}