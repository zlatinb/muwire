package com.muwire.gui

import com.muwire.core.Persona

import static com.muwire.gui.Translator.trans

import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

import com.muwire.core.SharedFile
import com.muwire.core.collections.FileCollection
import com.muwire.core.collections.FileCollectionItem

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
    
    JTable hitsTable
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")
        dialog = new JDialog(mainFrame,trans("COLLECTION_TOOL_TITLE", model.collection.name),true)
        dialog.setResizable(true)
        
        mainPanel = builder.panel {
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                label(text : trans("COLLECTION_VIEWS"))
            }
            scrollPane(constraints : BorderLayout.CENTER) {
                hitsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.hits) {
                        closureColumn(header : trans("SEARCHER"), preferredWidth : 100, type : Persona, read : {it.searcher})
                        closureColumn(header : trans("TIMESTAMP"), preferredWidth : 100, type : Long, read : {it.timestamp})
                    }
                }
            }
            panel(constraints : BorderLayout.SOUTH) {
                button(text : trans("CLEAR_HITS"), clearHitsAction)
            }
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        // hits table
        hitsTable.setDefaultRenderer(Long.class, new DateRenderer())
        hitsTable.setDefaultRenderer(Persona.class, new PersonaCellRenderer())
        hitsTable.rowSorter.setComparator(0, new PersonaComparator())
        
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