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
import javax.swing.JTextPane
import javax.swing.ListSelectionModel
import javax.swing.ScrollPaneConstants
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
    
    ContactChooser contactChooser
    ContactChooserModel contactChooserModel

    JPanel component
    
    private UISettings settings
    
    void initUI() {
        settings = application.context.get("ui-settings")

        contactChooserModel= new ContactChooserModel(model.core.trustService.getGood().values())
        contactChooser = new ContactChooser(settings, contactChooserModel)
        
        component = builder.panel {
            borderLayout()
            widget(contactChooser, constraints: BorderLayout.CENTER, 
                    border : builder.titledBorder(title : trans("CONTACT_CHOOSER_SELECT_CONTACT"),
                    border : builder.etchedBorder(), titlePosition : TitledBorder.TOP))
        }
    }
    
    void mvcGroupInit(Map<String, String> args) {
        contactChooser.loadPOPs(model.allContacts)
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
