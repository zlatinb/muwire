package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans

import java.awt.BorderLayout
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.DataHelper

import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.TransferHandler
import javax.swing.border.TitledBorder
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class CollectionWizardView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    CollectionWizardModel model

    def window
    JPanel mainPanel
    
    def nameTextField 
    def commentTextArea
    JTable filesTable
    def lastFilesTableSortEvent
    JTree jTree
    
    void initUI() {
        
        int rowHeight = application.context.get("row-height")

        window = builder.frame(visible: false, locationRelativeTo : null,
            defaultCloseOperation : JFrame.DISPOSE_ON_CLOSE,
            iconImage : builder.imageIcon("/MuWire-48x48.png").image){
            mainPanel = panel {
                cardLayout()
                panel(constraints : "configuration") {
                    gridLayout(rows : 2, cols : 1)
                    panel {
                        borderLayout()
                        panel(constraints : BorderLayout.NORTH,
                        border : titledBorder(title: trans("COLLECTION_NAME"), border : etchedBorder(), titlePosition : TitledBorder.TOP)) {
                            borderLayout()
                            nameTextField = textField(constraints : BorderLayout.CENTER)
                        }
                        panel(constraints : BorderLayout.CENTER,
                        border : titledBorder(title: trans("COLLECTION_DESCRIPTION"), border : etchedBorder(), titlePosition : TitledBorder.TOP)) {
                            borderLayout()
                            scrollPane(constraints : BorderLayout.CENTER) {
                                commentTextArea = textArea(editable : true, columns : 100, lineWrap : true, wrapStyleWord : true)
                            }
                        }
                    }
                    panel {
                        borderLayout()
                        panel(constraints : BorderLayout.NORTH) {
                            gridLayout(rows : 1, cols :3)
                            panel  {
                                label(text : trans("COLLECTION_TOTAL_FILES") + ":" + model.files.size())
                                label(text : trans("COLLECTION_TOTAL_SIZE") + ":" + DataHelper.formatSize2Decimal(model.totalSize(), false) + trans("BYTES_SHORT"))
                            }
                            panel {
                                label(text : trans("COLLECTION_DND"))
                            }
                            panel{}
                        }
                        scrollPane(constraints : BorderLayout.CENTER) {
                            filesTable = table(id : "files-table", autoCreateRowSorter : true, rowHeight : rowHeight, fillsViewportHeight : true) {
                                tableModel(list : model.files) {
                                    closureColumn(header : trans("NAME"), type : String, read : {it.getCachedPath()})
                                    closureColumn(header : trans("SIZE"), type : Long, preferredWidth: 30, read : {it.getCachedLength()})
                                    closureColumn(header : trans("COMMENT"), type : Boolean, preferredWidth : 20, read : {it.getComment() != null})
                                }
                            }
                        }
                        panel(constraints : BorderLayout.SOUTH) {
                            button(text : trans("CANCEL"), cancelAction)
                            button(text : trans("REVIEW"), reviewAction)
                        }
                    }
                }
                panel(constraints : "review") {
                    borderLayout()
                    panel (constraints : BorderLayout.NORTH) {
                        label(text : trans("COLLECTION_REVIEW_TITLE"))
                    }
                    scrollPane(constraints : BorderLayout.CENTER) {
                        jTree = new JTree(model.tree)
                        jTree.setCellRenderer(new PathTreeRenderer())
                        tree(id : "preview-tree", rowHeight : rowHeight, rootVisible : true, expandsSelectedPaths : true, jTree)
                    }
                    panel(constraints : BorderLayout.SOUTH) {
                        button(text : trans("CANCEL"), cancelAction)
                        button(text : trans("PREVIOUS"), previousAction)
                        button(text : trans("COPY_HASH_TO_CLIPBOARD"), copyHashAction)
                        button(text : trans("SAVE"), saveAction)
                    }
                }
            }
        }        
    }
    
    void switchToReview() {
        mainPanel.getLayout().show(mainPanel, "review")
        TreeUtil.expand(jTree)
    }
    
    void switchToConfiguration() {
        mainPanel.getLayout().show(mainPanel, "configuration")
    }
    
    void warnMissingName() {
        JOptionPane.showMessageDialog(window, trans("COLLECTION_NAME_WARNING"), 
            trans("COLLECTION_NAME_WARNING_TITLE"), JOptionPane.WARNING_MESSAGE)
    }
    
    void mvcGroupInit(Map<String,String> args) {
        filesTable.setDefaultRenderer(Long.class, new SizeRenderer())
        filesTable.setTransferHandler(new SFTransferHandler())
        filesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        filesTable.rowSorter.addRowSorterListener({evt -> lastFilesTableSortEvent = evt})
        
        JPopupMenu filesMenu = new JPopupMenu()
        JMenuItem removeItem = new JMenuItem(trans("REMOVE"))
        removeItem.addActionListener({removeSelectedFiles()})
        filesMenu.add(removeItem)
        
        filesTable.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3)
                    filesMenu.show(e.getComponent(), e.getX(), e.getY())
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3)
                    filesMenu.show(e.getComponent(), e.getX(), e.getY())
            }
        })
        
        window.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        window.pack()
        window.setVisible(true)
    }
    
    void removeSelectedFiles() {
        int[] selected = filesTable.getSelectedRows()
        if (selected.length == 0)
            return
        if (lastFilesTableSortEvent != null) {
            for (int i = 0; i < selected.length; i++)
                selected[i] = filesTable.rowSorter.convertRowIndexToModel(selected[i])
        }
        
        Arrays.sort(selected)
        for(int i = selected.length - 1; i >= 0; i--) {
            def sf = model.files.remove(selected[i])
            model.uniqueFiles.remove(sf)
        }
        filesTable.model.fireTableDataChanged()
    }
    
    private class SFTransferHandler extends TransferHandler {
        @Override
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            for (DataFlavor df : transferFlavors) {
                if (df == CopyPasteSupport.LIST_FLAVOR) {
                    return true
                }
            }
            return false
        }
        
        public boolean importData(JComponent c, Transferable t) {
            
            List<?> items = t.getTransferData(CopyPasteSupport.LIST_FLAVOR)
            if (items == null || items.isEmpty()) {
                return false
            }
            
            items.each { 
                if (model.uniqueFiles.add(it)) {
                    model.files.add(it)
                    filesTable.model.fireTableDataChanged()
                }
            }
        }
    }
}