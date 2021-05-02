package com.muwire.gui

import com.muwire.core.collections.FileCollection
import com.muwire.core.messenger.MWMessage
import com.muwire.core.messenger.MWMessageAttachment
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.inject.Inject
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.annotation.Nonnull
import javax.swing.ListSelectionModel
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.TransferHandler
import javax.swing.table.DefaultTableCellRenderer
import java.awt.BorderLayout
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.stream.Collectors

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class MessageFolderView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    MessageFolderModel model
    @MVCMember @Nonnull
    MessageFolderController controller
    @Inject @Nonnull GriffonApplication application

    JTable messageHeaderTable, messageAttachmentsTable
    JTextArea messageBody
    JSplitPane messageSplitPane
    
    def lastMessageHeaderTableSortEvent
    def lastMessageAttachmentsTableSortEvent
    
    JPanel folderPanel
    void initUI() {
        int rowHeight = application.context.get("row-height")
        folderPanel = builder.panel (constraints: model.name) {
            gridLayout(rows: 1, cols: 1)
            splitPane(orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, dividerLocation: 500) {
                scrollPane {
                    table(id: "message-header-table", autoCreateRowSorter: true, rowHeight: rowHeight,
                        dragEnabled: true, transferHandler: new MessageExportTransferHandler()) {
                        if (!model.outgoing) {
                            tableModel(list: model.messageHeaders) {
                                closureColumn(header: trans("SENDER"), preferredWidth: 200, type: String, read: { it.message.sender.getHumanReadableName() })
                                closureColumn(header: trans("SUBJECT"), preferredWidth: 300, type: String, read: { it.message.subject })
                                closureColumn(header: trans("RECIPIENTS"), preferredWidth: 20, type: Integer, read: { it.message.recipients.size() })
                                closureColumn(header: trans("DATE"), preferredWidth: 50, type: Long, read: { it.message.timestamp })
                                closureColumn(header: trans("UNREAD"), preferredWidth: 20, type: Boolean, read: { it.status })
                            }
                        } else {
                            tableModel(list : model.messageHeaders) {
                                closureColumn(header: trans("RECIPIENTS"), preferredWidth: 400, type: String, read : {
                                    it.message.recipients.stream().map({it.getHumanReadableName()}).collect(Collectors.joining(","))
                                })
                                closureColumn(header: trans("SUBJECT"), preferredWidth: 300, type: String, read: { it.message.subject })
                                closureColumn(header: trans("DATE"), preferredWidth: 50, type: Long, read: { it.message.timestamp })
                            }
                        }
                    }
                }
                panel {
                    borderLayout()
                    panel(constraints: BorderLayout.CENTER) {
                        borderLayout()
                        panel(constraints: BorderLayout.NORTH) {
                            borderLayout()
                            panel(constraints: BorderLayout.WEST) {
                                label(text: trans("RECIPIENTS") + ":")
                                label(text: bind { model.messageRecipientList })
                            }
                        }
                        splitPane(id: "message-attachments-split-pane", orientation: JSplitPane.VERTICAL_SPLIT,
                                continuousLayout: true, dividerLocation: 500, constraints: BorderLayout.CENTER) {
                            scrollPane {
                                textArea(id: "message-body-textarea", editable: false, lineWrap: true, wrapStyleWord: true)
                            }
                            panel {
                                borderLayout()
                                scrollPane(constraints: BorderLayout.CENTER) {
                                    table(id: "message-attachments-table", autoCreateRowSorter: true, rowHeight: rowHeight) {
                                        tableModel(list: model.messageAttachments) {
                                            closureColumn(header: trans("NAME"), preferredWidth: 300, type: String, read: { it.name })
                                            closureColumn(header: trans("SIZE"), preferredWidth: 20, type: Long, read: {
                                                if (it instanceof MWMessageAttachment)
                                                    return it.length
                                                else
                                                    return it.totalSize()
                                            })
                                            closureColumn(header: trans("COLLECTION"), preferredWidth: 20, type: Boolean, read: {
                                                it instanceof FileCollection
                                            })
                                        }
                                    }
                                }
                                panel(constraints: BorderLayout.EAST) {
                                    gridBagLayout()
                                    button(text: trans("DOWNLOAD"), enabled: bind { model.messageAttachmentsButtonEnabled },
                                            constraints: gbc(gridx: 0, gridy: 0), downloadAttachmentAction)
                                    button(text: trans("DOWNLOAD_ALL"), enabled: bind { model.messageAttachmentsButtonEnabled },
                                            constraints: gbc(gridx: 0, gridy: 1), downloadAllAttachmentsAction)
                                }
                            }
                        }
                    }
                    panel(constraints: BorderLayout.SOUTH) {
                        button(text: trans("COMPOSE"), messageComposeAction)
                        button(text: trans("REPLY"), enabled: bind { model.messageButtonsEnabled }, messageReplyAction)
                        button(text: trans("REPLY_ALL"), enabled: bind { model.messageButtonsEnabled }, messageReplyAllAction)
                        button(text: trans("DELETE"), enabled: bind { model.messageButtonsEnabled }, messageDeleteAction)
                    }
                }
            }
        }
        
        messageHeaderTable = builder.getVariable("message-header-table")
        messageBody = builder.getVariable("message-body-textarea")
        messageSplitPane = builder.getVariable("message-attachments-split-pane")
        messageAttachmentsTable = builder.getVariable("message-attachments-table")
    }

    int[] selectedMessageHeaders() {
        int[] selectedRows = messageHeaderTable.getSelectedRows()
        if (selectedRows.length == 0)
            return selectedRows
        if (lastMessageHeaderTableSortEvent != null) {
            for (int i = 0; i < selectedRows.length; i++)
                selectedRows[i] = messageHeaderTable.rowSorter.convertRowIndexToModel(selectedRows[i])
        }
        selectedRows
    }

    List<?> selectedMessageAttachments() {
        int[] rows = messageAttachmentsTable.getSelectedRows()
        if (rows.length == 0)
            return Collections.emptyList()
        if (lastMessageAttachmentsTableSortEvent != null) {
            for (int i = 0; i < rows.length; i++) {
                rows[i] = messageAttachmentsTable.rowSorter.convertRowIndexToModel(rows[i])
            }
        }
        List rv = new ArrayList()
        for (int i = 0; i < rows.length; i++)
            rv.add(model.messageAttachments.get(rows[i]))
        rv
    }
    
    void removeMessages(Set<MWMessage> set) {
        for(Iterator<MWMessageStatus> iter = model.messageHeaders.iterator(); iter.hasNext();) {
            def status = iter.next()
            if (set.contains(status.message)) {
                model.messages.remove(status)
                iter.remove()
            }
        }
        messageHeaderTable.model.fireTableDataChanged()
    }
    
    void mvcGroupInit(Map<String, String> args) {
        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        
        messageHeaderTable.setDefaultRenderer(Integer.class, centerRenderer)
        messageHeaderTable.setDefaultRenderer(Long.class, new DateRenderer())
        messageHeaderTable.rowSorter.addRowSorterListener({evt -> lastMessageHeaderTableSortEvent = evt})
        messageHeaderTable.rowSorter.setSortsOnUpdates(true)
        def sortKey = new RowSorter.SortKey(model.outgoing ? 2 : 3, SortOrder.ASCENDING)
        messageHeaderTable.rowSorter.setSortKeys(Collections.singletonList(sortKey))
        
        def selectionModel = messageHeaderTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener({
            int[] selectedRows = selectedMessageHeaders()
            if (selectedRows.length == 0) {
                model.messageButtonsEnabled = false
                model.messageAttachmentsButtonEnabled = false
                messageBody.setText("")
                model.messageRecipientList = ""
            } else {
                MWMessageStatus selectedStatus = model.messageHeaders.get(selectedRows[0])
                controller.markMessageRead(selectedStatus)
                MWMessage selected = selectedStatus.message
                messageBody.setText(selected.body)
                model.messageButtonsEnabled = true
                model.messageRecipientList = String.join(",", selected.recipients.collect {it.getHumanReadableName()})

                if (selected.attachments.isEmpty() && selected.collections.isEmpty()) {
                    messageSplitPane.setDividerLocation(1.0d)
                    model.messageAttachments.clear()
                    messageAttachmentsTable.model.fireTableDataChanged()
                } else {
                    messageSplitPane.setDividerLocation(0.7d)
                    model.messageAttachments.clear()
                    model.messageAttachments.addAll(selected.attachments)
                    model.messageAttachments.addAll(selected.collections)
                    messageAttachmentsTable.model.fireTableDataChanged()
                }
            }

        })

        JPopupMenu messagesMenu = new JPopupMenu()
        JMenuItem replyMenuItem = new JMenuItem(trans("REPLY"))
        replyMenuItem.addActionListener({controller.messageReply()})
        messagesMenu.add(replyMenuItem)
        JMenuItem replyAllMenuItem = new JMenuItem(trans("REPLY_ALL"))
        replyAllMenuItem.addActionListener({controller.messageReplyAll()})
        messagesMenu.add(replyAllMenuItem)
        JMenuItem deleteMenuItem = new JMenuItem(trans("DELETE"))
        deleteMenuItem.addActionListener({controller.messageDelete()})
        messagesMenu.add(deleteMenuItem)
        JMenuItem copyIdFromMessageItem = new JMenuItem(trans("COPY_FULL_ID"))
        copyIdFromMessageItem.addActionListener({controller.copyIdFromMessage()})
        messagesMenu.add(copyIdFromMessageItem)
        messageHeaderTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    showPopupMenu(messagesMenu, e)
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    showPopupMenu(messagesMenu, e)
            }
        })


        messageAttachmentsTable.setDefaultRenderer(Long.class, new SizeRenderer())
        messageAttachmentsTable.rowSorter.addRowSorterListener({evt -> lastMessageAttachmentsTableSortEvent = evt})
        messageAttachmentsTable.rowSorter.setSortsOnUpdates(true)
        selectionModel = messageAttachmentsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener({
            List selected = selectedMessageAttachments()
            if (selected.isEmpty()) {
                model.messageAttachmentsButtonEnabled = false
            } else {
                model.messageAttachmentsButtonEnabled = true
            }
        })
    }

    private static void showPopupMenu(JPopupMenu menu, MouseEvent event) {
        menu.show(event.getComponent(), event.getX(), event.getY())
    }
    
    private class MessageExportTransferHandler extends TransferHandler {
        
        @Override
        protected Transferable createTransferable(JComponent c) {
            if (c != messageHeaderTable)
                return null
            int [] selectedRows = messageHeaderTable.getSelectedRows()
            if (selectedRows.length == 0)
                return null
            
            if (lastMessageHeaderTableSortEvent != null) {
                for (int i = 0; i < selectedRows.length; i++) {
                    selectedRows[i] = messageHeaderTable.rowSorter.convertRowIndexToModel(selectedRows[i])
                }
            }
            List<MWMessage> toTransfer = new ArrayList<>()
            selectedRows.each {
                MWMessage message = model.messageHeaders[it].message
                toTransfer.add(new MWMessageTransferable(message: message, from: model.name))
            }
            
            return new MWTransferable(toTransfer)
        }
        
        @Override
        public int getSourceActions(JComponent c) {
            return LINK | COPY | MOVE
        }
    }
}