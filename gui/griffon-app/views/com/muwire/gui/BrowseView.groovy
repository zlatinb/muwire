package com.muwire.gui

import griffon.core.artifact.GriffonView

import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.TreePath
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent

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
    
    ResultTree resultsTree
    def treeExpansions = new TreeExpansions()

    JTable resultsTable
    def lastSortEvent
    
    def sequentialDownloadCheckbox
    
    private boolean onDemandExpansionRegistered
    private Set<TreePath> fullyExpandedPaths = new HashSet<>()
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        int treeRowHeight = application.context.get("tree-row-height")
        
        p = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.NORTH) {
                borderLayout()
                panel(constraints: BorderLayout.CENTER) {
                    label(text: trans("STATUS") + ":")
                    label(text: bind { trans(model.status.name()) })
                    label(text: bind {
                        model.currentBatch == 0 ? "" :
                                "$model.resultCount/$model.currentBatch (" + Math.round(model.resultCount * 100 / model.currentBatch)+ "%)"
                    })
                    label(text : trans("BROWSE_TOTAL_FILES") + ":")
                    label(text: bind {model.totalResults})
                }
                panel(constraints: BorderLayout.EAST) {
                    button(text: trans("VIEW_PROFILE"), toolTipText: trans("TOOLTIP_VIEW_PROFILE"), viewProfileAction)
                }
            }
            resultsPanel = panel(constraints: BorderLayout.CENTER) {
                cardLayout()
                panel(constraints: "table") {
                    borderLayout()
                    scrollPane(constraints: BorderLayout.CENTER) {
                        resultsTable = table(id: "results-table", autoCreateRowSorter: true, rowHeight: rowHeight) {
                            tableModel(list: model.results) {
                                closureColumn(header: trans("NAME"), preferredWidth: 350, type: UIResultEvent, read: { it })
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
                        resultsTree = new ResultTree(model.resultsTreeModel)
                        tree(id: "results-tree", rowHeight: treeRowHeight, resultsTree)
                    }
                }
            }
            panel (constraints : BorderLayout.SOUTH) {
                gridLayout(rows: 1, cols: 4)
                panel(border: etchedBorder()) {
                    buttonGroup(id: "viewType")
                    radioButton(text: trans("TREE"), toolTipText: trans("TOOLTIP_RESULT_VIEW_TREE"),
                            selected: true, buttonGroup: viewType, actionPerformed: showTree)
                    radioButton(text: trans("TABLE"), toolTipText: trans("TOOLTIP_RESULT_VIEW_TABLE"),
                            selected: false, buttonGroup: viewType, actionPerformed: showTable)
                }
                panel(border: etchedBorder()) {
                    button(text: trans("DOWNLOAD"), toolTipText: trans("TOOLTIP_DOWNLOAD_FILE"),
                            enabled: bind { model.downloadActionEnabled }, downloadAction)
                    label(text: trans("DOWNLOAD_SEQUENTIALLY"), toolTipText: trans("TOOLTIP_DOWNLOAD_SEQUENTIALLY"),
                            enabled: bind {model.downloadActionEnabled})
                    sequentialDownloadCheckbox = checkBox(enabled: bind {model.downloadActionEnabled})
                }
                panel(border: etchedBorder()) {
                    button(text: trans("VIEW_DETAILS"), toolTipText: trans("TOOLTIP_VIEW_DETAILS_RESULT"),
                            enabled: bind {model.viewDetailsActionEnabled}, viewDetailsAction)
                }
                panel(border: etchedBorder()) {
                    def textField = new JTextField(15)
                    textField.addActionListener({ controller.filter() })
                    widget(id: "filter-field", enabled: bind { model.filterEnabled }, textField)
                    button(text: trans("FILTER"), toolTipText: trans("TOOLTIP_FILTER_RESULTS"),
                            enabled: bind { model.filterEnabled }, filterAction)
                    button(text: trans("CLEAR"), toolTipText: trans("TOOLTIP_FILTER_CLEAR"),
                            enabled: bind { model.clearFilterEnabled}, clearFilterAction)
                }
            }
        }
        
        p.registerKeyboardAction(closeTab,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW)

        Action downloadAction = new AbstractAction() {
            @Override
            void actionPerformed(ActionEvent e) {
                controller.download()
            }
        }
        ["results-tree", "results-table"].each {
            JComponent resultsComponent = builder.getVariable(it)
            resultsComponent.registerKeyboardAction(downloadAction,
                    KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                    JComponent.WHEN_FOCUSED)
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
        resultsTree.setSharedPredicate(controller.core.fileManager::isShared, controller.core.downloadManager::isDownloading)
        resultsTree.addTreeExpansionListener(treeExpansions)
        resultsTree.addMouseListener(mouseListener)
        resultsTree.addTreeSelectionListener({
            model.downloadActionEnabled = false
            model.viewDetailsActionEnabled = false
            TreePath[] selected = resultsTree.selectionModel.getSelectionPaths()
            if (selected == null || selected.length == 0) {
                return
            }
            
            model.downloadActionEnabled = true
            UIResultEvent result = resultsTree.singleResultSelected()
            if (result != null) {
                model.viewDetailsActionEnabled = true
            }
        })
        
        // results table
        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        resultsTable.setDefaultRenderer(Integer.class,centerRenderer)

        resultsTable.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())
        
        resultsTable.setDefaultRenderer(UIResultEvent.class, 
                new ResultNameTableCellRenderer(controller.core.fileManager::isShared,
                        controller.core.downloadManager::isDownloading,
                        true))
        
        resultsTable.rowSorter.addRowSorterListener({evt -> lastSortEvent = evt})
        resultsTable.rowSorter.setSortsOnUpdates(true)
        resultsTable.rowSorter.setComparator(0, new UIResultEventComparator(true))
        
        def selectionModel = resultsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener({
            int[] rows = resultsTable.getSelectedRows()
            if (rows.length == 0) {
                model.downloadActionEnabled = false
                model.viewDetailsActionEnabled = false
                return
            }
            
            if (lastSortEvent != null) {
                for (int i = 0; i < rows.length; i ++) {
                    rows[i] = resultsTable.rowSorter.convertRowIndexToModel(rows[i])
                }
            }
            
            model.viewDetailsActionEnabled = rows.length == 1
            model.downloadActionEnabled = true
            
        })
        resultsTable.addMouseListener(mouseListener)
        
        p.putClientProperty("focusListener", new FocusListener())
    }
    
    private void showMenu(MouseEvent e) {
        if (!RightClickSupport.processRightClick(e))
            return
        
        JPopupMenu menu = new JPopupMenu()
        if (model.downloadActionEnabled) {
            JMenuItem download = new JMenuItem(trans("DOWNLOAD"))
            download.addActionListener({controller.download()})
            menu.add(download)
        }
        if (model.viewDetailsActionEnabled) {
            JMenuItem viewDetails = new JMenuItem(trans("VIEW_DETAILS"))
            viewDetails.addActionListener({controller.viewDetails()})
            menu.add(viewDetails)
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
        
        if (model.treeVisible && model.session != null && 
                model.session.supportsIncremental() && resultsTree.folderSelected()) {
            JMenuItem expand = new JMenuItem(trans("EXPAND_FULLY"))
            expand.addActionListener({
                List<TreePath> selected = resultsTree.selectedFolderPaths()
                selected.each {controller.requestFetch(it, true)}
                fullyExpandedPaths.addAll(selected)
                selected.each {TreeUtil.expandNode(resultsTree, it.getLastPathComponent())}
            })
            menu.add(expand)
        }
        
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
    
    void clearForFilter() {
        resultsTable.clearSelection()
        treeExpansions.manualExpansion = false
        treeExpansions.expandedPaths.clear()
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
        if (model.session == null && !model.session.supportsIncremental()) {
            TreeUtil.expand(tree)
        } else if (!onDemandExpansionRegistered) {
            onDemandExpansionRegistered = true
            tree.addTreeExpansionListener(new OnDemandTreeExpansion())
        }
        if (treeExpansions.manualExpansion)
            expanded.each { tree.expandPath(it) }
        fullyExpandedPaths.each {TreeUtil.expandNode(tree, it.getLastPathComponent())}
        tree.setSelectionPaths(selectedPaths)
    }
    
    void updateUIs() {
        resultsTable.updateUI()
        resultsTree.updateUI()
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
            return rv
        }
    }

    /**
     * If the tree is visible, return a list with tuple (result, targetFile, parentFile)
     */
    List<ResultAndTargets> decorateResults(List<UIResultEvent> results) {
        List<ResultAndTargets> rv = new ArrayList<>()
        if (!model.treeVisible) {
            // flat
            for(UIResultEvent event : results)
                rv << new ResultAndTargets(event, new File(event.name), null)
        } else {
           rv.addAll(resultsTree.decorateResults(results))
        }
        rv
    }
    
    void showUnexpandedFolderWarning() {
        JOptionPane.showMessageDialog(null, trans("WARNING_UNEXPANDED_FOLDER_BODY"),
            trans("WARNING_UNEXPANDED_FOLDER_TITLE"), JOptionPane.WARNING_MESSAGE)
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
            boolean oldVisible = model.visible
            model.visible = focus
            if (!oldVisible && focus)
                controller.displayBatchedResults()
        }
    }
    
    private class OnDemandTreeExpansion implements TreeExpansionListener {

        @Override
        void treeExpanded(TreeExpansionEvent event) {
            controller.requestFetch(event.getPath(), false)
        }

        @Override
        void treeCollapsed(TreeExpansionEvent event) {
            fullyExpandedPaths.remove(event.getPath())
        }
    }
}