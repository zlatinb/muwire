package com.muwire.gui

import com.muwire.core.Persona
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.ListSelectionModel
import javax.swing.TransferHandler
import javax.swing.border.TitledBorder
import java.awt.BorderLayout
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class ContactSelectorView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ContactSelectorModel model
    
    DefaultListModel contactsModel
    JList contactsList

    JPanel component
    
    void initUI() {
        contactsModel = new DefaultListModel()
        model.contacts.each {
            contactsModel.addElement(new Contact(it))
        }
        contactsList = new JList(contactsModel)
        contactsList.setVisibleRowCount(2)
        
        component = builder.panel(border : builder.titledBorder(title : trans("RECIPIENTS_TITLE"),
                border : builder.etchedBorder(), titlePosition : TitledBorder.TOP)) {
            borderLayout()
            scrollPane(constraints : BorderLayout.CENTER) {
                widget(contactsList)
            }
        }
    }
    
    void mvcGroupInit(Map<String, String> args) {
        def transferHandler = new PersonaTransferHandler()
        contactsList.setTransferHandler(transferHandler)
        contactsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)

        JPopupMenu contactsMenu = new JPopupMenu()
        JMenuItem removeItem = new JMenuItem(trans("REMOVE"))
        removeItem.addActionListener({removeSelectedContacts()})
        contactsMenu.add(removeItem)

        contactsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3)
                    contactsMenu.show(e.getComponent(), e.getX(), e.getY())
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3)
                    contactsMenu.show(e.getComponent(), e.getX(), e.getY())
            }
        })
    }
    
    void removeSelectedContacts() {
        int [] selected = contactsList.getSelectedIndices()
        if (selected.length == 0)
            return
        Arrays.sort(selected)
        for (int i = selected.length - 1; i >= 0; i--) {
            Contact removed = contactsModel.remove(selected[i])
            model.contacts.remove(removed.persona)
        }
    }

    class PersonaTransferHandler extends TransferHandler {
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
                if (model.contacts.add(it))
                    contactsModel.insertElementAt(new Contact(it),0)
            }
            return true
        }
    }

    private static class Contact {
        private final Persona persona
        Contact(Persona persona) {
            this.persona = persona
        }

        public String toString() {
            "<html>" + PersonaCellRenderer.htmlize(persona) + "</html>"
        }
    }
}
