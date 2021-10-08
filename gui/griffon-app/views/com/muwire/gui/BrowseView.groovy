package com.muwire.gui

import griffon.core.artifact.GriffonView

import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

import static com.muwire.gui.Translator.trans
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableCellRenderer

import com.muwire.core.search.UIResultEvent

import java.awt.BorderLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

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
    JPanel p, resultsPanel
    
    JTree resultsTree
    def treeExpansions = new TreeExpansions()
    
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
            resultsPanel = panel(constraints: BorderLayout.CENTER) {
                cardLayout()
                panel(constraints: "table") {
                    borderLayout()
                    scrollPane(constraints: BorderLayout.CENTER) {
                        resultsTable = table(autoCreateRowSorter: true, rowHeight: rowHeight) {
                            tableModel(list: model.results) {
                                closureColumn(header: trans("NAME"), preferredWidth: 350, type: String, read: { row -> HTMLSanitizer.sanitize(row.getFullPath()) })
                                closureColumn(header: trans("SIZE"), preferredWidth: 20, type: Long, read: { row -> row.size })
                                closureColumn(header: trans("COMMENTS"), preferredWidth: 20, type: Boolean, read: { row -> row.comment != null })
                                closureColumn(header: trans("CERTIFICATES"), preferredWidth: 20, type: Integer, read: { row -> row.certificates })
                                closureColumn(header: trans("COLLECTIONS"), preferredWidth: 20, type: Integer, read: { row -> row.collections.size() })
                            }
                        }
                    }
                }
                panel(constraints: "tree") {
                    borderLayout()
                    scrollPane(constraints: BorderLayout.CENTER) {
                        resultsTree = new JTree(model.resultsTreeModel)
                        // TODO: custom renderer
                        tree(id: "results-tree", rowHeight: rowHeight, rootVisible: false, expandsSelectedPaths: true, 
                                largeModel: true, resultsTree)
                    }
                }
            }
            panel (constraints : BorderLayout.SOUTH) {
                gridLayout(rows: 1, cols: 4)
                panel(border: etchedBorder()) {
                    buttonGroup(id: "viewType")
                    radioButton(text: trans("TREE"), selected: true, buttonGroup: viewType, actionPerformed: showTree)
                    radioButton(text: trans("TABLE"), selected: false, buttonGroup: viewType, actionPerformed: showTable)
                }
                panel(border: etchedBorder()) {
                    button(text: trans("DOWNLOAD"), enabled: bind { model.downloadActionEnabled }, downloadAction)
                    label(text: trans("DOWNLOAD_SEQUENTIALLY"), enabled: bind {model.downloadActionEnabled})
                    sequentialDownloadCheckbox = checkBox(enabled: bind {model.downloadActionEnabled})
                }
                panel(border: etchedBorder()) {
                    button(text: trans("VIEW_COMMENT"), enabled: bind { model.viewCommentActionEnabled }, viewCommentAction)
                    button(text: trans("VIEW_CERTIFICATES"), enabled: bind { model.viewCertificatesActionEnabled }, viewCertificatesAction)
                    button(text: trans("VIEW_COLLECTIONS"), enabled: bind { model.viewCollectionsActionEnabled }, viewCollectionsAction)
                }
                panel(border: etchedBorder()) {
                    def textField = new JTextField(15)
                    textField.addActionListener({ controller.filter() })
                    widget(id: "filter-field", enabled: bind { model.filterEnabled }, textField)
                    button(text: trans("FILTER"), enabled: bind { model.filterEnabled }, filterAction)
                    button(text: trans("CLEAR"), enabled: bind { model.filterEnabled }, clearFilterAction)
                }
            }
        }
        
        // right-click menu
        def mouseListener = new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e)
            }
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e)
            }
        }
        
        // results tree
        JTree resultsTree = builder.getVariable("results-tree")
        resultsTree.addTreeExpansionListener(treeExpansions)
        resultsTree.setCellRenderer(new ResultTreeRenderer())
        resultsTree.addMouseListener(mouseListener)
        resultsTree.addTreeSelectionListener({
            model.downloadActionEnabled = false
            model.viewCertificatesActionEnabled = false
            model.viewCommentActionEnabled = false
            model.viewCollectionsActionEnabled = false
            TreePath[] selected = resultsTree.selectionModel.getSelectionPaths()
            if (selected == null || selected.length == 0) {
                return
            }
            
            model.downloadActionEnabled = true
            if (selected.length == 1) {
                def userObject = selected[0].getLastPathComponent().getUserObject()
                if (userObject instanceof UIResultEvent) {
                    UIResultEvent result = (UIResultEvent) userObject
                    model.viewCommentActionEnabled = result.comment != null
                    model.viewCollectionsActionEnabled = !result.collections.isEmpty()
                    model.viewCertificatesActionEnabled = result.certificates > 0
                }
            }
        })
        
        // results table
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
                model.viewCollectionsActionEnabled = false
                return
            }
            
            if (lastSortEvent != null) {
                for (int i = 0; i < rows.length; i ++) {
                    rows[i] = resultsTable.rowSorter.convertRowIndexToModel(rows[i])
                }
            }
            
            boolean downloadActionEnabled = true
            
            model.viewCertificatesActionEnabled = (rows.length == 1 && model.results[rows[0]].certificates > 0)
            model.viewCollectionsActionEnabled = rows.length == 1 && model.results[rows[0]].collections.size() > 0
            
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
        resultsTable.addMouseListener(mouseListener)
        
        p.putClientProperty("focusListener", new FocusListener())
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
        if (model.viewCollectionsActionEnabled) {
            JMenuItem viewCollections = new JMenuItem(trans("VIEW_COLLECTIONS"))
            viewCollections.addActionListener({controller.viewCollections()})
            menu.add(viewCollections)
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
        mainFrameGroup.model.browses.add(model.host.getHumanReadableName())
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
        
        showTree.call()
    }
    
    void refreshResults() {
        int [] selectedRows = resultsTable.getSelectedRows()
        if (lastSortEvent != null) {
            for (int i = 0; i < selectedRows.length; i ++)
                selectedRows[i] = resultsTable.rowSorter.convertRowIndexToModel(selectedRows[i])
        }
        resultsTable.model.fireTableDataChanged()
        if (lastSortEvent != null) {
            for (int i = 0; i < selectedRows.length; i ++)
                selectedRows[i] = resultsTable.rowSorter.convertRowIndexToView(selectedRows[i])
        }
        for (int row : selectedRows)
            resultsTable.selectionModel.addSelectionInterval(row, row)
        
        
        JTree tree = builder.getVariable("results-tree")
        TreePath[] selectedPaths = tree.getSelectionPaths()
        Set<TreePath> expanded = new HashSet<>(treeExpansions.expandedPaths)
        model.resultsTreeModel.nodeStructureChanged(model.root)
        
        if (treeExpansions.manualExpansion)
            expanded.each { tree.expandPath(it) }
        else
            TreeUtil.expand(tree)
        tree.setSelectionPaths(selectedPaths)
    }
    
    void expandUnconditionally() {
        JTree tree = builder.getVariable("results-tree")
        TreeUtil.expand(tree)
    }
    
    def selectedResults() {
        if (model.treeVisible) {
            JTree tree = builder.getVariable("results-tree")
            List<UIResultEvent> rv = new ArrayList<>()
            for (TreePath path : tree.getSelectionPaths()) {
                TreeUtil.getLeafs(path.getLastPathComponent(), rv)
            }
            return rv
        } else {
            int[] rows = resultsTable.getSelectedRows()
            if (rows.length == 0)
                return null
            if (lastSortEvent != null) {
                for (int i = 0; i < rows.length; i++) {
                    rows[i] = resultsTable.rowSorter.convertRowIndexToModel(rows[i])
                }
            }

            List<UIResultEvent> rv = new ArrayList<>()
            for (Integer i : rows)
                rv << model.results[i]
            rv
        }
    }

    /**
     * If the tree is visible, return a mapping from an UIResultEvent object
     * to a File object that should be it's target download based on the selected
     * node in the tree.
     */
    Map<UIResultEvent, File> decorateResults(List<UIResultEvent> results) {
        Map<UIResultEvent, File> rv = new HashMap<>()
        if (!model.treeVisible) {
            // flat
            for(UIResultEvent event : results)
                rv.put(event, new File(event.name))
        } else {
            TreePath[] paths = resultsTree.getSelectionPaths()
            for (TreePath path : paths) {
                def node = path.getLastPathComponent()
                def userObject = node.getUserObject()
                
                if (userObject instanceof UIResultEvent) {
                    // a leaf is selected
                    rv.put(userObject, new File(userObject.name))
                } else {
                    Set<TreePath> subPaths = new HashSet<>()
                    TreeUtil.subPaths(path, subPaths)
                    final int start = path.getPathCount() - 1
                    for (TreePath subPath : subPaths) {
                        File target = new File("")
                        for (int i = start; i < subPath.getPathCount() - 1; i ++) {
                            def subNode = subPath.getPathComponent(i)
                            if (subNode.isLeaf())
                                target = new File(target, subNode.getUserObject().name)
                            else
                                target = new File(target, subNode.getUserObject().toString())
                        }
                        UIResultEvent event = subPath.getLastPathComponent().getUserObject()
                        if (results.contains(event)) 
                            rv.put(event, target)
                    }
                }
            }
        }
        rv
    }
    
    def showTree = {
        model.treeVisible = true
        resultsPanel.getLayout().show(resultsPanel, "tree")
    }
    
    def showTable = {
        model.treeVisible = false
        resultsPanel.getLayout().show(resultsPanel, "table")
    }
    
    def closeTab = {
        def mainFrameGroup = application.mvcGroupManager.findGroup("MainFrame")
        mainFrameGroup.model.browses.remove(model.host.getHumanReadableName())
        
        int index = parent.indexOfTab(model.host.getHumanReadableName())
        parent.removeTabAt(index)
        model.downloadActionEnabled = false
        mvcGroup.destroy()
    }
    
    private class FocusListener {
        void onFocus(boolean focus) {
            model.visible = focus
        }
    }
}