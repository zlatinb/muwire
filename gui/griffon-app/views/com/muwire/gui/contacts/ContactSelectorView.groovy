package com.muwire.gui.contacts

import com.muwire.core.Persona
import com.muwire.gui.CopyPasteSupport
import com.muwire.gui.PersonaCellRenderer
import com.muwire.gui.UISettings
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.inject.Inject
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
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class ContactSelectorView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ContactSelectorModel model
    @Inject
    GriffonApplication application
    
    DefaultListModel contactsModel
    JList contactsList
    ContactChooser contactChooser
    ContactChooserModel contactChooserModel

    JPanel component
    
    private UISettings settings
    
    private Contact lastSelectedContact
    
    void initUI() {
        settings = application.context.get("ui-settings")
        
        contactsModel = new DefaultListModel()
        model.contacts.each {
            contactsModel.addElement(new Contact(it, settings))
        }
        contactsList = new JList(contactsModel)
        contactsList.setVisibleRowCount(2)
        
        contactChooserModel= new ContactChooserModel(model.core.trustService.getGood().values())
        contactChooser = new ContactChooser(settings, contactChooserModel)
        
        component = builder.panel {
            borderLayout()
            scrollPane(constraints : BorderLayout.CENTER, border : builder.titledBorder(title : trans("RECIPIENTS_TITLE"),
                    border : builder.etchedBorder(), titlePosition : TitledBorder.TOP)) {
                widget(contactsList)
            }
            widget(contactChooser, constraints: BorderLayout.SOUTH, 
                    border : builder.titledBorder(title : trans("CONTACT_CHOOSER_SELECT_CONTACT"),
                    border : builder.etchedBorder(), titlePosition : TitledBorder.TOP))
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
        
        contactChooser.addItemListener(new ItemListener() {
            @Override
            void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED)
                    return
                Object item = e.getItem()
                if (item == null || item instanceof String)
                    return
                ContactChooserPOP ccp = (ContactChooserPOP) item
                if (ccp.getPersona() == null)
                    return
          
                lastSelectedContact = new Contact(ccp.getPersona(), settings)
            }
        })
        
        contactChooser.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ENTER)
                    return
                if (lastSelectedContact != null) {
                    if (model.contacts.add(lastSelectedContact.persona))
                        contactsModel << lastSelectedContact
                }
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
                    contactsModel.insertElementAt(new Contact(it, settings),0)
            }
            return true
        }
    }

    private static class Contact {
        private final UISettings settings
        private final Persona persona
        Contact(Persona persona, UISettings settings) {
            this.persona = persona
            this.settings = settings
        }

        public String toString() {
            if (settings.personaRendererIds)
                return "<html>" + PersonaCellRenderer.htmlize(persona) + "</html>"
            else
                return PersonaCellRenderer.justName(persona)
        }
    }
}
