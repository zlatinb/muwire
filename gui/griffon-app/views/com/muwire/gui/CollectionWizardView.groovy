package com.muwire.gui

import com.google.common.collect.Sets
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

    JFrame window
    JPanel mainPanel
    
    def nameTextField 
    def commentTextArea
    JTable filesTable
    def lastFilesTableSortEvent
    JTree jTree
    
    def mainFrame
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")

        window = builder.frame(visible: false, locationRelativeTo : mainFrame,
            defaultCloseOperation : JFrame.DISPOSE_ON_CLOSE,
            iconImage : builder.imageIcon("/MuWire-48x48.png").image,
            preferredSize: [800,800]){
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
                            gridBagLayout()
                            panel(constraints: gbc(gridx: 0, gridy: 0, weightx: 100))  {
                                label(text : bind { trans("COLLECTION_TOTAL_FILES") + ":" + model.numFiles })
                                label(text : bind { trans("COLLECTION_TOTAL_SIZE") + ":" + formatSize(model.totalSize) })
                            }
                            panel(constraints: gbc(gridx: 1, gridy:0, gridwidth: 2, weightx: 100)) {
                                label(text : trans("COLLECTION_DND"))
                            }
                        }
                        scrollPane(constraints : BorderLayout.CENTER) {
                            filesTable = table(id : "files-table", autoCreateRowSorter : true, rowHeight : rowHeight, fillsViewportHeight : true) {
                                tableModel(list : model.files) {
                                    closureColumn(header : trans("NAME"), type : String, read : {it.getCachedPath()})
                                    closureColumn(header : trans("SIZE"), type : Long, read : {it.getCachedLength()})
                                    closureColumn(header : trans("COMMENT"), type : Boolean, read : {it.getComment() != null})
                                }
                            }
                        }
                        panel(constraints : BorderLayout.SOUTH) {
                            button(text : trans("CANCEL"), cancelAction)
                            button(text : trans("REVIEW"), enabled: bind {model.numFiles > 0}, reviewAction)
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
        TableUtil.packColumns(filesTable, Sets.newHashSet(0,1))
        TableUtil.sizeColumn(filesTable, 1)
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
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    if (!RightClickSupport.processRightClick(e))
                        return
                    filesMenu.show(e.getComponent(), e.getX(), e.getY())
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    if (!RightClickSupport.processRightClick(e))
                        return
                    filesMenu.show(e.getComponent(), e.getX(), e.getY())
                }
            }
        })
        
        window.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        window.pack()
        window.setLocationRelativeTo(mainFrame)
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
            model.numFiles--
            model.totalSize -= sf.getCachedLength()
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
                    model.numFiles++
                    model.totalSize += it.getCachedLength()
                    filesTable.model.fireTableDataChanged()
                }
            }
        }
    }

    private static String formatSize(long size) {
        StringBuffer sb = new StringBuffer(32)
        String bTrans = trans("BYTES_SHORT")
        SizeFormatter.format(size,sb)
        sb.append(bTrans)
        sb.toString()
    }
}