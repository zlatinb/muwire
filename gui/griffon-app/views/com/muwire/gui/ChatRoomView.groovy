package com.muwire.gui

import com.google.common.collect.Sets
import com.muwire.core.trust.TrustLevel
import com.muwire.gui.chat.ChatEntry
import com.muwire.gui.chat.ChatEntryPane
import com.muwire.core.mulinks.MuLink
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.PersonaOrProfileCellRenderer
import com.muwire.gui.profile.PersonaOrProfileComparator
import com.muwire.gui.profile.ProfileConstants
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonView
import griffon.core.mvc.MVCGroup

import javax.inject.Inject
import javax.swing.JMenu
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.text.SimpleAttributeSet
import java.awt.Dimension
import java.awt.Point
import java.util.function.Consumer

import static com.muwire.gui.Translator.trans
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSplitPane
import javax.swing.JTextPane
import javax.swing.ListSelectionModel
import javax.swing.text.Element
import javax.swing.text.Style
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.StyledDocument

import com.muwire.core.Persona

import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class ChatRoomView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ChatRoomModel model
    @MVCMember @Nonnull
    ChatRoomController controller
    @Inject
    GriffonApplication application

    ChatNotificator chatNotificator
    
    def pane
    def parent
    ChatEntryPane sayField
    JTextPane roomTextArea
    def textScrollPane
    JTable membersTable
    def lastMembersTableSortEvent
    UISettings settings
    
    private final Consumer<MuLink> muLinkConsumer = new MuLinkConsumer()
    
    void initUI() {
        settings = application.context.get("ui-settings")
        int rowHeight = application.context.get("row-height")
        def parentModel = mvcGroup.parentGroup.model
        
        sayField = new ChatEntryPane(settings, model.members)
        
        if (model.console || model.privateChat) {
            pane = builder.panel {
                borderLayout()
                panel(constraints : BorderLayout.CENTER) {
                    gridLayout(rows : 1, cols : 1)
                    textScrollPane = scrollPane {
                        roomTextArea = textPane(editable : false)
                    }
                }
                panel(constraints : BorderLayout.SOUTH) {
                    borderLayout()
                    label(text : trans("SAY_SOMETHING_HERE") + ": ", constraints : BorderLayout.WEST)
                    scrollPane(verticalScrollBarPolicy: JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, constraints: BorderLayout.CENTER) {
                        widget(sayField, enabled: bind { parentModel.sayActionEnabled }, actionPerformed: { controller.say() })
                    }
                }
            }
        } else {
            pane = builder.panel {
                borderLayout()
                panel(constraints : BorderLayout.CENTER) {
                    gridLayout(rows : 1, cols : 1)
                    splitPane(orientation : JSplitPane.HORIZONTAL_SPLIT, continuousLayout : true, dividerLocation : 300) {
                        panel {
                            gridLayout(rows : 1, cols : 1)
                            scrollPane {
                                membersTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                                    tableModel(list : model.members) {
                                        closureColumn(header : trans("NAME"), preferredWidth: 150, type: PersonaOrProfile, read : {it})
                                        closureColumn(header : trans("TRUST_STATUS"), preferredWidth: 10, type : TrustLevel, 
                                                read : {model.core.trustService.getLevel(it.getPersona().destination)})
                                    }
                                }
                            }
                        }
                        panel {
                            gridLayout(rows : 1, cols : 1)
                            textScrollPane = scrollPane(horizontalScrollBarPolicy: JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
                                roomTextArea = textPane(editable : false)
                            }
                        }
                    }
                }
                panel(constraints : BorderLayout.SOUTH, minimumSize: [0, ProfileConstants.MAX_THUMBNAIL_SIZE * 2] as Dimension) {
                    borderLayout()
                    label(text : trans("SAY_SOMETHING_HERE") + ": ", constraints : BorderLayout.WEST)
                    scrollPane(verticalScrollBarPolicy: JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, constraints: BorderLayout.CENTER) {
                        widget(sayField, enabled: bind { parentModel.sayActionEnabled }, actionPerformed: { controller.say() })
                    }
                }

            }
        }
        
        SmartScroller smartScroller = new SmartScroller(textScrollPane)
        pane.putClientProperty("mvcId", mvcGroup.mvcId)
    }
    
    void mvcGroupInit(Map<String,String> args) {
        parent = mvcGroup.parentGroup.view.builder.getVariable(model.tabName)
        parent.addTab(model.roomTabName, pane)
        
        int index = parent.indexOfComponent(pane)
        parent.setSelectedIndex(index)
        
        def tabPanel = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.CENTER) {
                label(text : model.roomTabName)
            }
            button(icon : imageIcon("/close_tab.png"), preferredSize: [20, 20], constraints : BorderLayout.EAST,
                actionPerformed : closeTab )
        }
        if (!model.console)
            parent.setTabComponentAt(index, tabPanel)
            
        if (membersTable != null) {
            
            TableUtil.packColumns(membersTable, Sets.newHashSet(0))
            TableUtil.nicknameColumn(membersTable, 0)
            membersTable.setDefaultRenderer(TrustLevel.class, new TrustCellRenderer())
            membersTable.setDefaultRenderer(PersonaOrProfile.class, new PersonaOrProfileCellRenderer(application.context.get("ui-settings")))
            membersTable.rowSorter.setComparator(0, new PersonaOrProfileComparator())
            membersTable.rowSorter.addRowSorterListener({evt -> lastMembersTableSortEvent = evt})
            membersTable.rowSorter.setSortsOnUpdates(true)
            membersTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            
            membersTable.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            if (e.button == MouseEvent.BUTTON1 && e.clickCount > 1) {
                                controller.privateMessage()
                            } else if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                                showPopupMenu(e)
                        }
                        
                        public void mouseReleased(MouseEvent e) {
                            if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                                showPopupMenu(e)
                        }
                    })
        }
        
        // styles
        StyledDocument document = roomTextArea.getStyledDocument()
        Style regular = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE)
        Style italic = document.addStyle("italic", regular)
        StyleConstants.setItalic(italic, true)
        Style gray = document.addStyle("gray", regular)
        StyleConstants.setForeground(gray, Color.GRAY)
        Style red = document.addStyle("red", regular)
        StyleConstants.setForeground(red, Color.RED)
        Style green = document.addStyle("green", regular)
        StyleConstants.setForeground(green, Color.GREEN)


        SimpleAttributeSet sab = new SimpleAttributeSet()
        StyleConstants.setAlignment(sab, StyleConstants.ALIGN_LEFT)
        roomTextArea.setParagraphAttributes(sab, false)
    }
    
    private void showPopupMenu(MouseEvent e) {
        if (!RightClickSupport.processRightClick(e))
            return
        JPopupMenu menu = new JPopupMenu()
        JMenuItem privateChat = new JMenuItem(trans("START_PRIVATE_CHAT"))
        privateChat.addActionListener({controller.privateMessage()})
        menu.add(privateChat)
        menu.addSeparator()
        JMenuItem viewProfile = new JMenuItem(trans("VIEW_PROFILE"))
        viewProfile.addActionListener({controller.viewProfile()})
        menu.add(viewProfile)
        menu.addSeparator()
        JMenuItem browse = new JMenuItem(trans("BROWSE"))
        browse.addActionListener({controller.browse()})
        menu.add(browse)

        menu.addSeparator()
        
        JMenu otherActionsMenu = new JMenu(trans("OTHER_ACTIONS"))
        JMenuItem markTrusted = new JMenuItem(trans("MARK_TRUSTED"))
        markTrusted.addActionListener({controller.markTrusted()})
        otherActionsMenu.add(markTrusted)
        JMenuItem markNeutral = new JMenuItem(trans("MARK_NEUTRAL"))
        markNeutral.addActionListener({controller.markNeutral()})
        otherActionsMenu.add(markNeutral)
        JMenuItem markDistrusted = new JMenuItem(trans("MARK_DISTRUSTED"))
        markDistrusted.addActionListener({controller.markDistrusted()})
        otherActionsMenu.add(markDistrusted)
        menu.add(otherActionsMenu)
        
        menu.show(e.getComponent(), e.getX(), e.getY())
    }
    
    Persona getSelectedPersona() {
        getSelectedPOP()?.getPersona()
    }
    
    PersonaOrProfile getSelectedPOP() {
        int selectedRow = membersTable.getSelectedRow()
        if (selectedRow < 0)
            return null
        if (lastMembersTableSortEvent != null)
            selectedRow = membersTable.rowSorter.convertRowIndexToModel(selectedRow)
        model.members[selectedRow]
    }
    
    void refreshMembersTable() {
        int selectedRow = membersTable.getSelectedRow()
        membersTable.model.fireTableDataChanged()
        membersTable.selectionModel.setSelectionInterval(selectedRow, selectedRow)
    }
    
    def closeTab = {
        int index = parent.indexOfComponent(pane)
        parent.removeTabAt(index)
        controller.leaveRoom()
        chatNotificator.roomClosed(mvcGroup.mvcId)
        mvcGroup.destroy()
    }
    
    void appendGray(String gray) {
        StyledDocument doc = roomTextArea.getStyledDocument()
        doc.insertString(doc.getEndPosition().getOffset() - 1, gray, doc.getStyle("gray"))
    }
    
    void appendRed(String red) {
        StyledDocument doc = roomTextArea.getStyledDocument()
        doc.insertString(doc.getEndPosition().getOffset() - 1, red, doc.getStyle("red"))
    }

    void appendGreen(String green) {
        StyledDocument doc = roomTextArea.getStyledDocument()
        doc.insertString(doc.getEndPosition().getOffset() - 1, green, doc.getStyle("green"))
    }
    
    void appendSay(String text, PersonaOrProfile sender, long timestamp) {
        
        if (settings.chatNotifyMentions &&
                sender.getPersona() != model.core.me &&
                text.contains("@${model.core.me.toBase64()}@"))
            chatNotificator.notifyMention(model.room, model.host.getHumanReadableName())

        
        boolean trusted = model.core.trustService.getLevel(sender.getPersona().getDestination()) == TrustLevel.TRUSTED
        def textField = new ChatEntry(text, settings, model::getByPersona, muLinkConsumer, timestamp, sender, trusted)
        
        
        StyledDocument doc = roomTextArea.getStyledDocument()
        def style = doc.addStyle("newStyle", null)
        StyleConstants.setComponent(style, textField)
        doc.insertString(doc.getEndPosition().getOffset() - 1, " ", style)
        doc.insertString(doc.getEndPosition().getOffset() - 1, "\n", doc.getStyle("regular"))
        controller.trimLines()
    }
    
    int getLineCount() {
        StyledDocument doc = roomTextArea.getStyledDocument()
        doc.getDefaultRootElement().getElementCount() - 1
    }
    
    void removeFirstLine() {
        StyledDocument doc = roomTextArea.getStyledDocument()
        Element element = doc.getParagraphElement(0)
        doc.remove(0, element.getEndOffset())
    }
    
    private class MuLinkConsumer implements Consumer<MuLink> {

        @Override
        void accept(MuLink muLink) {
            MVCGroup mainFrame = application.mvcGroupManager.findGroup("MainFrame")
            if (muLink.getLinkType() == MuLink.LinkType.FILE) {
                mainFrame.controller.downloadLink(muLink)
            } else if (muLink.getLinkType() == MuLink.LinkType.COLLECTION) {
                mainFrame.controller.fetchCollectionLink(muLink)
            }
        }
    }
}