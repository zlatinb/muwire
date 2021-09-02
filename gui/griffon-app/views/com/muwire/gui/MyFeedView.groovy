package com.muwire.gui

import com.muwire.core.SharedFile
import griffon.core.artifact.GriffonView

import javax.swing.RowSorter
import javax.swing.SortOrder

import static com.muwire.gui.Translator.trans
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.ListSelectionModel

import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class MyFeedView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    MyFeedModel model
    @MVCMember @Nonnull
    MyFeedController controller

    def mainFrame
    def dialog
    def itemsPanel
    def itemsTable
    def lastItemsTableSortEvent
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")
        dialog = new JDialog(mainFrame,trans("MY_FEED"),true)
        dialog.setResizable(true)
        
        itemsPanel = builder.panel(preferredSize: [800,300]) {
            borderLayout()
            panel (constraints : BorderLayout.NORTH) {
                label(text : trans("PUBLISHED_FILES") + " ")
                label(text : bind {model.itemsCount})
            }
            scrollPane( constraints : BorderLayout.CENTER ) {
                itemsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.items) {
                        closureColumn(header : trans("NAME"), preferredWidth: 500, type : String, read : { HTMLSanitizer.sanitize(it.getCachedPath())})
                        closureColumn(header : trans("SIZE"), preferredWidth: 100, type : Long, read : {it.getCachedLength()})
                        closureColumn(header : trans("DATE"), preferredWidth: 200, type : Long, read : {it.getPublishedTimestamp()})
                    }
                }
            }
            panel (constraints : BorderLayout.SOUTH) {
                button(text : trans("UNPUBLISH"), enabled : bind {model.unpublishActionEnabled}, unpublishAction)
                button(text : trans("CLOSE"), closeAction)
            }
        }
        
        itemsTable.rowSorter.addRowSorterListener({evt -> lastItemsTableSortEvent = evt})
        itemsTable.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())
        itemsTable.columnModel.getColumn(2).setCellRenderer(new DateRenderer())
        
        def selectionModel = itemsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener({
            model.unpublishActionEnabled = !selectedItems()?.isEmpty()
        })
        
        itemsTable.addMouseListener(new MouseAdapter() {
            void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e)
            }
             void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e)
            }
        })
        
        def sortKey = new RowSorter.SortKey(2, SortOrder.ASCENDING)
        itemsTable.rowSorter.setSortKeys(Collections.singletonList(sortKey))
    }
    
    private void showMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu()
        JMenuItem unpublish = new JMenuItem(trans("UNPUBLISH"))
        unpublish.addActionListener({controller.unpublish()})
        menu.add(unpublish)
        
        menu.show(e.getComponent(), e.getX(), e.getY())
    }

    
    List<SharedFile> selectedItems() {
        int[] rows = itemsTable.getSelectedRows()
        if (rows.length == 0)
            return null
        if (lastItemsTableSortEvent != null) {
            for (int i = 0; i < rows.length; i++)
                rows[i] = itemsTable.rowSorter.convertRowIndexToModel(rows[i])
        }
        List<SharedFile> rv = []
        for (int row : rows)
            rv << model.items[row]
        rv
    }
    
    void refreshItemsTable() {
        itemsTable.model.fireTableDataChanged()
        if (model.items.isEmpty())
            model.unpublishActionEnabled = false
    }
    
    void mvcGroupInit(Map<String,String> args) {
        dialog.with {
            getContentPane().add(itemsPanel)
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
}