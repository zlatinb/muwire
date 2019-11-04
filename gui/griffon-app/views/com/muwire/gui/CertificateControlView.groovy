package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.swing.JDialog
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

import com.muwire.core.Persona
import com.muwire.core.filecert.Certificate

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class CertificateControlView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    CertificateControlModel model

    def mainFrame
    def dialog
    def panel
    def usersTable
    def certsTable
    def lastUsersSortEvent
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")
        dialog = new JDialog(mainFrame,"Certificates",true)
        dialog.setResizable(true)
        
        panel = builder.panel {
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                label("Certificates in your repository")
            }
            panel (constraints : BorderLayout.CENTER) {
                gridLayout(rows : 1, cols : 2)
                scrollPane {
                    usersTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                        tableModel(list : model.users) {
                            closureColumn(header : "Issuer", type : String, read : {it.getHumanReadableName()})
                        }
                    }
                }
                scrollPane {
                    certsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                        tableModel(list : model.certificates) {
                            closureColumn(header : "File Name", type : String, read : {it.name.name})
                            closureColumn(header : "Hash", type : String, read : {Base64.encode(it.infoHash.getRoot())})
                            closureColumn(header : "Timestamp", type : String, read : {
                                def date = new Date(it.timestamp)
                                date.toString()
                            })
                        }
                    }
                }
            }
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        usersTable.rowSorter.addRowSorterListener({evt -> lastUsersSortEvent = evt})
        def selectionModel = usersTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            Persona issuer = getSelectedIssuer()
            if (issuer == null)
                return
            Set<Certificate> certs = model.core.certificateManager.byIssuer.get(issuer)
            if (certs == null)
                return
            model.certificates.clear()
            model.certificates.addAll(certs)
            certsTable.model.fireTableDataChanged()
        })
        
        dialog.getContentPane().add(panel)
        dialog.pack()
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        dialog.show()
    }
    
    private Persona getSelectedIssuer() {
        int selectedRow = usersTable.getSelectedRow()
        if (selectedRow < 0)
            return null
        if (lastUsersSortEvent != null)
            selectedRow = usersTable.rowSorter.convertRowIndexToModel(selectedRow)
        model.users[selectedRow]
    }
    
}