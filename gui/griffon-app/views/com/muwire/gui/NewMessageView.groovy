package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JList
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ListModel
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.TransferHandler
import javax.swing.TransferHandler.TransferSupport
import javax.swing.border.TitledBorder

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.SharedFile
import com.muwire.core.messenger.MWMessageAttachment

import java.awt.BorderLayout
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class NewMessageView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    NewMessageModel model

    JFrame window
    JTextField subjectField
    JTextArea bodyArea
    JTable attachmentsTable
    def lastAttachmentsTableSortEvent
    def mainFrame

    MVCGroup contactSelector
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")
        
        def params = [:]
        params.contacts = model.recipients
        params.core = model.core
        params.contactsPOP = model.allPops
        contactSelector = mvcGroup.createMVCGroup("contact-selector", UUID.randomUUID().toString(), params)
        
        window = builder.frame(visible : false, locationRelativeTo : mainFrame,
            defaultCloseOperation : JFrame.DISPOSE_ON_CLOSE,
            iconImage : builder.imageIcon("/MuWire-48x48.png").image, 
                preferredSize: [800, 600]){
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                borderLayout()
                panel(constraints : BorderLayout.NORTH) {
                    gridLayout(rows: 1, cols: 1)
                    widget(contactSelector.view.component)
                }
                panel(constraints : BorderLayout.SOUTH, border : titledBorder(title : trans("SUBJECT"),
                    border : etchedBorder(), titlePosition : TitledBorder.TOP)) {
                    borderLayout()
                    subjectField = textField(constraints : BorderLayout.CENTER)
                }
            }
            panel(constraints : BorderLayout.CENTER) {
                gridLayout(rows : 1, cols : 1)
                splitPane(orientation : JSplitPane.VERTICAL_SPLIT, continuousLayout : true, dividerLocation : 300) {
                    scrollPane(border : titledBorder(title : trans("MESSAGE_NOUN"), border : etchedBorder(), titlePosition : TitledBorder.TOP)) {
                        bodyArea = textArea(editable : true, rows : 10, columns : 50, lineWrap : true, wrapStyleWord : true)
                    }
                    panel (border : titledBorder(title : trans("ATTACHMENT_DROP_TABLE_TITLE"),
                        border : etchedBorder(), titlePosition : TitledBorder.TOP)) {
                        borderLayout()
                        scrollPane(constraints : BorderLayout.CENTER) {
                            attachmentsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) { 
                                tableModel(list : model.attachments) {
                                    closureColumn(header : trans("NAME"), type : String, read : {it.name})
                                    closureColumn(header : trans("SIZE"), type : Long, read : {
                                        if (it instanceof MWMessageAttachment)
                                            return it.length
                                        else 
                                            return it.totalSize()
                                    })
                                }
                            }
                        }
                    }
                }
            }
            panel(constraints : BorderLayout.SOUTH) {
                button(text : trans("SEND"), sendAction)
                button(text : trans("CANCEL"), cancelAction)
            }
        }
    }
    
    void mvcGroupInit(Map<String, String> args) {
        subjectField.setText(model.replySubject)
        bodyArea.setText(model.replyBody)
        
        // attachments table
        def transferHandler = new AttachmentTransferHandler()
        attachmentsTable.setTransferHandler(transferHandler)
        attachmentsTable.setFillsViewportHeight(true)
        attachmentsTable.setDefaultRenderer(Long.class, new SizeRenderer())
        attachmentsTable.rowSorter.setSortsOnUpdates(true)
        attachmentsTable.rowSorter.addRowSorterListener({evt -> lastAttachmentsTableSortEvent = evt})
        attachmentsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        
        JPopupMenu attachmentsMenu = new JPopupMenu()
        JMenuItem removeAttachmentItem = new JMenuItem(trans("REMOVE"))
        removeAttachmentItem.addActionListener({removeSelectedAttachments()})
        attachmentsMenu.add(removeAttachmentItem)
        attachmentsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3)
                    attachmentsMenu.show(e.getComponent(), e.getX(), e.getY())
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3)
                    attachmentsMenu.show(e.getComponent(), e.getX(), e.getY())
            }
        })
        
        // general window 
        
        window.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        window.pack()
        window.setLocationRelativeTo(mainFrame)
        window.setVisible(true)
    }
    
    void mvcGroupDestroy() {
        contactSelector?.destroy()
    }
    
    void removeSelectedAttachments() {
        int [] selected = attachmentsTable.getSelectedRows()
        if (selected.length == 0)
            return
        if (lastAttachmentsTableSortEvent != null) {
            for (int i = 0; i < selected.length; i++)
                selected[i] = attachmentsTable.rowSorter.convertRowIndexToModel(selected[i])
        }
        Arrays.sort(selected)
        for (int i = selected.length - 1; i >= 0; i--)
            model.attachments.remove(selected[i])
        attachmentsTable.model.fireTableDataChanged()
            
    }
    
    class AttachmentTransferHandler extends TransferHandler {
        
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
                def attachment
                if (it instanceof SharedFile)
                    attachment = new MWMessageAttachment(new InfoHash(it.getRoot()), it.file.getName(), it.getCachedLength(), (byte)it.pieceSize)
                else
                    attachment = it
                model.attachments.add(attachment)
                
            }
            attachmentsTable.model.fireTableDataChanged()
            true
        }
    }
    

}