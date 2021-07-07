package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JTabbedPane
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
class SharedFileView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    SharedFileModel model
    @MVCMember @Nonnull
    SharedFileController controller

    def mainFrame
    def dialog
    def panel
    def searchersPanel
    def searchersTable
    def downloadersPanel
    def certificatesTable
    def certificatesPanel
    def lastCertificateSortEvent
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")
        dialog = new JDialog(mainFrame,trans("DETAILS_FOR",model.sf.getFile().getName()),true)
        dialog.setResizable(true)
        
        searchersPanel = builder.panel {
            borderLayout()
            scrollPane(constraints : BorderLayout.CENTER) {
                searchersTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.searchers) {
                        closureColumn(header : trans("SEARCHER"), type : String, read : {it.searcher?.getHumanReadableName()})
                        closureColumn(header : trans("QUERY"), type : String, read : {HTMLSanitizer.sanitize(it.query)})
                        closureColumn(header : trans("TIMESTAMP"), type : Long, read : {it.timestamp})
                    }
                }
            }
        }
        
        downloadersPanel = builder.panel {
            borderLayout()
            scrollPane(constraints : BorderLayout.CENTER) {
                table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.downloaders) {
                        closureColumn(header : trans("DOWNLOADER"), type : String, read : {it})
                    }
                }
            }
        }
        
        certificatesPanel = builder.panel {
            borderLayout()
            scrollPane(constraints : BorderLayout.CENTER) {
                certificatesTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.certificates) {
                        closureColumn(header : trans("ISSUER"), type:String, read : {it.issuer.getHumanReadableName()})
                        closureColumn(header : trans("FILE_NAME"), type : String, read : {HTMLSanitizer.sanitize(it.name.name)})
                        closureColumn(header : trans("COMMENT"), type : Boolean, read : {it.comment != null})
                        closureColumn(header : trans("TIMESTAMP"), type : Long, read : {it.timestamp})
                    }
                }
            }
            panel(constraints : BorderLayout.SOUTH) {
                button(text : trans("VIEW_COMMENT"), enabled : bind {model.showCommentActionEnabled}, showCommentAction)
            }
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {

        certificatesTable.rowSorter.addRowSorterListener({evt -> lastCertificateSortEvent = evt})
        def selectionModel = certificatesTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            Certificate c = getSelectedCertificate()
            model.showCommentActionEnabled = c != null && c.comment != null
        })
        
        certificatesTable.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e)
            }
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e)
            }
        })
        
        certificatesTable.setDefaultRenderer(Long.class, new DateRenderer())
        
        
        searchersTable.setDefaultRenderer(Long.class, new DateRenderer())
        
        def tabbedPane = new JTabbedPane()
        tabbedPane.addTab(trans("SEARCH_HITS"), searchersPanel)
        tabbedPane.addTab(trans("DOWNLOADERS"), downloadersPanel)
        tabbedPane.addTab(trans("CERTIFICATES"), certificatesPanel)
        
        dialog.with { 
            getContentPane().add(tabbedPane)
            pack()
            setLocationRelativeTo(mainFrame)
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
            addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                    mvcGroup.destroy()
                }
            })
            show()
        }
    }
    
    Certificate getSelectedCertificate() {
        int selectedRow = certificatesTable.getSelectedRow()
        if (selectedRow < 0)
            return null
        if (lastCertificateSortEvent != null) 
            selectedRow = certificatesTable.rowSorter.convertRowIndexToModel(selectedRow)
        model.certificates[selectedRow]
    }
    
    private void showMenu(MouseEvent e) {
        if (!model.showCommentActionEnabled)
            return
        JPopupMenu menu = new JPopupMenu()
        JMenuItem showComment = new JMenuItem(trans("VIEW_COMMENT"))
        showComment.addActionListener({controller.showComment()})
        menu.add(showComment)
        menu.show(e.getComponent(), e.getX(), e.getY())
    }
}