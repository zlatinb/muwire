package com.muwire.gui

import static com.muwire.gui.Translator.trans
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

import com.muwire.core.collections.FileCollection

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
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        p = builder.panel {
            gridLayout(rows : 3, cols: 1)
            panel {
                borderLayout()
                panel(constraints : BorderLayout.NORTH) {
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
            panel {}
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
            FileCollection selected = model.collections.get(row)
            model.comment = selected.comment
        })
    }
    
    int selectedCollection() {
        int row = collectionsTable.getSelectedRow()
        if (row < 0)
            return row
        if (lastCollectionsTableSortEvent != null) 
            row = collectionsTable.rowSorter.convertRowIndexToModel(row)
        
        return row
    }
    
    def closeTab = {
        def mainFrameGroup = application.mvcGroupManager.findGroup("MainFrame")
        mainFrameGroup.model.collections.remove(model.uuid.toString())
        
        int index = parent.indexOfTab(model.uuid.toString())
        parent.removeTabAt(index)
        mvcGroup.destroy()
    }
}