package com.muwire.gui

import static com.muwire.gui.Translator.trans
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.JTree
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.tree.TreePath

import com.muwire.core.collections.FileCollection
import com.muwire.core.collections.FileCollectionItem

import java.awt.BorderLayout

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class CollectionTabView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    CollectionTabModel model

    def parent
    def p
    
    JTable collectionsTable
    def lastCollectionsTableSortEvent
    JTextArea commentArea
    def itemsPanel
    JTable itemsTable
    def lastItemsTableSortEvent
    JTree itemsTree
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        p = builder.panel {
            gridLayout(rows : 3, cols: 1)
            panel {
                borderLayout()
                panel(constraints : BorderLayout.NORTH) {
                    label(text : trans("STATUS") + ":")
                    label(text : bind {trans(model.status.name())})
                }
                scrollPane(constraints : BorderLayout.CENTER, border : etchedBorder()) {
                    collectionsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                        tableModel(list : model.collections) {
                            closureColumn(header: trans("NAME"), preferredWidth: 200, type : String, read : {it.name})
                            closureColumn(header: trans("AUTHOR"), preferredWidth: 200, type : String, read : {it.author.getHumanReadableName()})
                            closureColumn(header: trans("COLLECTION_TOTAL_FILES"), preferredWidth: 20, type: Integer, read : {it.numFiles()})
                            closureColumn(header: trans("COLLECTION_TOTAL_SIZE"), preferredWidth: 20, type: Long, read : {it.totalSize()})
                            closureColumn(header: trans("COMMENT"), preferredWidth: 20, type: Boolean, read: {it.comment != ""})
                            closureColumn(header: trans("CREATED"), preferredWidth: 50, type: Long, read: {it.timestamp})
                        }
                    }
                }
                panel(constraints : BorderLayout.SOUTH) {
                    button(text : trans("COLLECTION_DOWNLOAD"), enabled : bind{model.downloadCollectionButtonEnabled}, downloadCollectionAction)
                }
            }
            panel {
                borderLayout()
                panel(constraints: BorderLayout.NORTH) {
                    label(text : trans("DESCRIPTION"))
                }
                commentArea = textArea(text : bind {model.comment}, editable : false, lineWrap : true, wrapStyleWord : true, constraints : BorderLayout.CENTER,
                    border : etchedBorder())
            }
            panel {
                borderLayout()
                panel(constraints : BorderLayout.NORTH) {
                    label(text: trans("FILES"))
                }
                itemsPanel = panel(constraints : BorderLayout.CENTER) {
                    cardLayout()
                    panel(constraints : "table") {
                        borderLayout()
                        scrollPane(constraints : BorderLayout.CENTER, border : etchedBorder()) {
                            itemsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                                tableModel(list : model.items) {
                                    closureColumn(header: trans("NAME"), preferredWidth : 200, type : String, read :{
                                        String.join(File.separator, it.pathElements)
                                    })
                                    closureColumn(header : trans("SIZE"), preferredWidth : 20, type : Long, read : {it.length})
                                    closureColumn(header : trans("COMMENT"), preferredWidth : 20, type : Boolean, read : {it.comment != ""})
                                }
                            }
                        }
                    }
                    panel(constraints : "tree") {
                        borderLayout()
                        scrollPane(constraints : BorderLayout.CENTER, border : etchedBorder()) {
                            itemsTree = new JTree(model.fileTreeModel)
                            itemsTree.setCellRenderer(new PathTreeRenderer())
                            tree(rowHeight : rowHeight, rootVisible : true, expandsSelectedPaths : true, itemsTree)
                        }
                    }
                }
                panel(constraints : BorderLayout.SOUTH) {
                    gridLayout(rows : 1, cols : 3)
                    panel {
                        buttonGroup(id : "viewType")
                        radioButton(text : trans("TREE"), selected : true, buttonGroup : viewType, actionPerformed : showTree)
                        radioButton(text : trans("TABLE"), selected : false, buttonGroup : viewType, actionPerformed : showTable)
                    }
                    panel {
                        button(text : trans("DOWNLOAD"), enabled : bind {model.downloadItemButtonEnabled}, downloadAction)
                        button(text : trans("VIEW_COMMENT"), enabled : bind{model.viewCommentButtonEnabled}, viewCommentAction)
                    }
                    panel {}
                }
            }
        }
    }
    
    void mvcGroupInit(Map<String, String> args) {
        def mainFrameGroup = application.mvcGroupManager.findGroup("MainFrame")
        mainFrameGroup.model.collections.add(model.uuid.toString())
        parent = mainFrameGroup.view.builder.getVariable("result-tabs")
        parent.addTab(model.uuid.toString(), p)
        
        int index = parent.indexOfComponent(p)
        parent.setSelectedIndex(index)
     
        def tabPanel
        builder.with {
            tabPanel = panel {
                borderLayout()
                panel {
                    label(text : model.fileName, constraints : BorderLayout.CENTER)
                }
                button(icon : imageIcon("/close_tab.png"), preferredSize : [20,20], constraints : BorderLayout.EAST, // TODO: in osx is probably WEST
                    actionPerformed : closeTab )
            }
        }

        parent.setTabComponentAt(index, tabPanel)
        mainFrameGroup.view.showSearchWindow.call()
        
        // collections table
        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        collectionsTable.setDefaultRenderer(Integer.class, centerRenderer)
        
        collectionsTable.columnModel.getColumn(3).setCellRenderer(new SizeRenderer())
        collectionsTable.columnModel.getColumn(5).setCellRenderer(new DateRenderer())
        
        collectionsTable.rowSorter.addRowSorterListener({evt -> lastCollectionsTableSortEvent = evt})
        collectionsTable.rowSorter.setSortsOnUpdates(true)
        
        def selectionModel = collectionsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int row = selectedCollection()
            if (row < 0)
                return
            model.downloadCollectionButtonEnabled = true
            FileCollection selected = model.collections.get(row)
            model.comment = selected.comment
            
            model.items.clear()
            model.items.addAll(selected.files)
            itemsTable.model.fireTableDataChanged()
            
            model.root.removeAllChildren()
            TreeUtil.copy(model.root, selected.tree.root)
            itemsTree.model.nodeStructureChanged(model.root)
            TreeUtil.expand(itemsTree)
        })
        
        
        // items table
        itemsTable.setDefaultRenderer(Long.class, new SizeRenderer())
        itemsTable.rowSorter.addRowSorterListener({evt -> lastItemsTableSortEvent = evt})
        itemsTable.rowSorter.setSortsOnUpdates(true)
        selectionModel = itemsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener({
            List<FileCollectionItem> items = selectedItems()
            if (items.isEmpty()) {
                model.viewCommentButtonEnabled = false
                model.downloadItemButtonEnabled = false
                return
            }
            model.downloadItemButtonEnabled = true
            model.viewCommentButtonEnabled = items.size()== 1 && items.get(0).comment != ""
        })
        
        // items tree
        itemsTree.addTreeSelectionListener({
            List<FileCollectionItem> items = selectedItems()
            if (items.isEmpty()) {
                model.viewCommentButtonEnabled = false
                model.downloadItemButtonEnabled = false
                return
            }
            model.downloadItemButtonEnabled = true
            model.viewCommentButtonEnabled = items.size()== 1 && items.get(0).comment != ""
        })
        showTree.call()
    }
    
    int selectedCollection() {
        int row = collectionsTable.getSelectedRow()
        if (row < 0)
            return row
        if (lastCollectionsTableSortEvent != null) 
            row = collectionsTable.rowSorter.convertRowIndexToModel(row)
        
        return row
    }
    
    List<FileCollectionItem> selectedItems() {
        if (!model.treeVisible) {
            int [] rows = itemsTable.getSelectedRows()
            if (rows.length == 0)
                return Collections.emptyList()
            if (lastItemsTableSortEvent != null) {
                for (int i = 0; i < rows.length; i++)
                    rows[i] = itemsTable.rowSorter.convertRowIndexToModel(rows[i])
            }
            List<FileCollectionItem> rv = new ArrayList<>()
            for(int i = 0; i < rows.length; i++)
                rv.add(model.items.get(rows[i]))
            return rv
        } else {
            List<FileCollectionItem> rv = new ArrayList<>()
            for (TreePath path : itemsTree.getSelectionPaths())
                TreeUtil.getLeafs(path.getLastPathComponent(), rv)
            return rv
        }
    }
    
    def showTree = {
        model.treeVisible = true
        itemsPanel.getLayout().show(itemsPanel, "tree")
    }
    
    def showTable = {
        model.treeVisible = false
        itemsPanel.getLayout().show(itemsPanel, "table")
    }
    
    def closeTab = {
        def mainFrameGroup = application.mvcGroupManager.findGroup("MainFrame")
        mainFrameGroup.model.collections.remove(model.uuid.toString())
        
        int index = parent.indexOfTab(model.uuid.toString())
        parent.removeTabAt(index)
        mvcGroup.destroy()
    }
}