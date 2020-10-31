package com.muwire.gui

import static com.muwire.gui.Translator.trans

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

import com.muwire.core.SharedFile
import com.muwire.core.collections.FileCollection

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class CollectionsToolView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    CollectionsToolModel model
    @MVCMember @Nonnull
    CollectionsToolController controller

    def mainFrame
    def dialog
    def mainPanel
    
    JTable collectionsTable
    JTable filesTable
    
    def lastCollectionSortEvent
    def lastFilesSortEvent
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")
        dialog = new JDialog(mainFrame,trans("COLLECTION_TOOL_TITLE"),true)
        dialog.setResizable(true)
        
        mainPanel = builder.panel {
            gridLayout(rows : 2, cols : 1)
            panel {
                borderLayout()
                panel (constraints : BorderLayout.NORTH) {
                    label(text : trans("COLLECTION_TOOL_HEADER"))
                }
                scrollPane(constraints : BorderLayout.CENTER) {
                    collectionsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                        tableModel(list : model.collections) {
                            closureColumn(header : trans("NAME"), preferredWidth : 100, type : String, read : {it.name})
                            closureColumn(header : trans("AUTHOR"), preferredWidth : 100, type : String, read : {it.author.getHumanReadableName()})
                            closureColumn(header : trans("FILES"), preferredWidth: 10, type : Integer, read : {it.numFiles()})
                            closureColumn(header : trans("SIZE"), preferredWidth : 10, type : Long, read : {it.totalSize()})
                            closureColumn(header : trans("COMMENT"), preferredWidth : 10, type : Boolean, read : {it.comment != ""})
                            closureColumn(header : trans("CREATED"), preferredWidth : 30, type : Long, read : {it.timestamp})
                        }
                    }
                }
                panel(constraints : BorderLayout.SOUTH) {
                     button(text : trans("VIEW_COMMENT"), enabled : bind {model.viewCommentButtonEnabled}, viewCommentAction)
                     button(text : trans("DELETE"), enabled : bind {model.deleteButtonEnabled}, deleteAction)  
                }
            }
            panel {
                borderLayout()
                scrollPane(constraints : BorderLayout.CENTER) {
                    filesTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                        tableModel(list : model.files) {
                            closureColumn(header : trans("NAME"), preferredWidth : 200, type : String, read : {it.getCachedPath()})
                            closureColumn(header : trans("SIZE"), preferredWidth : 10, type : Long, read : {it.getCachedLength()})
                            closureColumn(header : trans("COMMENT"), preferredWidth : 10, type : Boolean, read : {it.getComment() != null})
                        }
                    }
                }
                panel(constraints : BorderLayout.SOUTH) {
                    button(text : trans("VIEW_COMMENT"), enabled : bind{model.viewFileCommentButtonEnabled}, viewFileCommentAction)
                    button(text : trans("CLOSE"), closeAction)
                }
            }
        }
    }
    
    int selectedCollectionRow() {
        int selectedRow = collectionsTable.getSelectedRow()
        if (selectedRow < 0)
            return -1
        if (lastCollectionSortEvent != null)
            selectedRow = collectionsTable.rowSorter.convertRowIndexToModel(selectedRow)
        selectedRow
    }
    
    int selectedFileRow() {
        int selectedRow = filesTable.getSelectedRow()
        if (selectedRow < 0)
            return -1
        if (lastFilesSortEvent != null)
            selectedRow = filesTable.rowSorter.convertRowIndexToModel(selectedRow)
        selectedRow
    }
    
    void clearFilesTable() {
        model.files.clear()
        filesTable.model.fireTableDataChanged()
    }
    
    void mvcGroupInit(Map<String,String> args) {
        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        collectionsTable.setDefaultRenderer(Integer.class,centerRenderer)
        collectionsTable.columnModel.getColumn(3).setCellRenderer(new SizeRenderer())
        collectionsTable.columnModel.getColumn(5).setCellRenderer(new DateRenderer())
        
        collectionsTable.rowSorter.addRowSorterListener({evt -> lastCollectionSortEvent = evt})

        def selectionModel = collectionsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = selectedCollectionRow()
            if (selectedRow < 0) {
                model.viewCommentButtonEnabled = false
                model.deleteButtonEnabled = false
                return
            }
            
            model.deleteButtonEnabled = true
            FileCollection collection = model.collections.get(selectedRow)
            model.viewCommentButtonEnabled = collection.getComment() != ""
            
            model.files.clear()
            collection.files.each { 
                SharedFile sf = model.fileManager.getRootToFiles().get(it.infoHash).first()
                model.files.add(sf)
            }
            
            filesTable.model.fireTableDataChanged()
        })   
        
        filesTable.setDefaultRenderer(Long.class, new SizeRenderer())
        filesTable.rowSorter.addRowSorterListener({evt -> lastFilesSortEvent = evt})
        filesTable.rowSorter.setSortsOnUpdates(true)
        selectionModel = filesTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = selectedFileRow()
            if (selectedRow < 0) {
                model.viewFileCommentButtonEnabled = false
                return
            }
            SharedFile sf = model.files.get(selectedRow)
            model.viewFileCommentButtonEnabled = sf.getComment() != null
        })
        
        dialog.getContentPane().add(mainPanel)
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
}