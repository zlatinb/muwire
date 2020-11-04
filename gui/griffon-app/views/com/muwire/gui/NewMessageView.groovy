package com.muwire.gui

import griffon.core.artifact.GriffonView

import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.TransferHandler
import javax.swing.TransferHandler.TransferSupport

import com.muwire.core.InfoHash
import com.muwire.core.SharedFile
import com.muwire.core.messenger.MWMessageAttachment

import java.awt.BorderLayout
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
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

    def window
    JTextField subjectField
    JTextArea bodyArea
    JTable attachmentsTable
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        
        window = builder.frame(visible : false, locationRelativeTo : null,
            defaultCloseOperation : JFrame.DISPOSE_ON_CLOSE,
            iconImage : builder.imageIcon("/MuWire-48x48.png").image){
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                borderLayout()
                panel(constraints : BorderLayout.NORTH) {
                    label(text : trans("RECIPIENT"))
                    label(text : model.recipient.getHumanReadableName())
                }
                panel(constraints : BorderLayout.SOUTH) {
                    borderLayout()
                    label(text : trans("SUBJECT"), constraints : BorderLayout.WEST)
                    subjectField = textField(constraints : BorderLayout.CENTER)
                }
            }
            panel(constraints : BorderLayout.CENTER) {
                splitPane(orientation : JSplitPane.VERTICAL_SPLIT, continuousLayout : true, dividerLocation : 300) {
                    scrollPane {
                        bodyArea = textArea(editable : true, rows : 10, columns : 50)
                    }
                    panel {
                        borderLayout()
                        panel(constraints : BorderLayout.NORTH) {
                            label(text : trans("ATTACHMENT_DROP_TABLE_TITLE"))
                        }
                        scrollPane(constraints : BorderLayout.CENTER) {
                            attachmentsTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) { 
                                tableModel(list : model.attachments) {
                                    closureColumn(header : trans("NAME"), type : String, read : {it.name})
                                    closureColumn(header : trans("SIZE"), type : Long, read : {it.length})
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
        def transferHandler = new AttachmentTransferHandler()
        attachmentsTable.setTransferHandler(transferHandler)
        attachmentsTable.setFillsViewportHeight(true)
        attachmentsTable.setDefaultRenderer(Long.class, new SizeRenderer())
        attachmentsTable.rowSorter.setSortsOnUpdates(true)
        
        bodyArea.setText(model.replyBody)
        
        window.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        window.pack()
        window.setVisible(true)
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
            
            List<SharedFile> sfs = t.getTransferData(CopyPasteSupport.LIST_FLAVOR)
            if (sfs == null) {
                return
            }
            
            sfs.each { 
                def attachment = new MWMessageAttachment(new InfoHash(it.getRoot()), it.file.getName(), it.getCachedLength(), (byte)it.pieceSize)
                model.attachments.add(attachment)
                
            }
            attachmentsTable.model.fireTableDataChanged()
            true
        }
    }
}