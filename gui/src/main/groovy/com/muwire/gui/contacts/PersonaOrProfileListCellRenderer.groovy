package com.muwire.gui.contacts

import com.muwire.gui.PersonaCellRenderer
import com.muwire.gui.UISettings
import com.muwire.gui.profile.PersonaOrProfile

import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer
import java.awt.Component

import static com.muwire.gui.Translator.trans

class PersonaOrProfileListCellRenderer implements ListCellRenderer<PersonaOrProfile>{

    private final UISettings settings
    
    PersonaOrProfileListCellRenderer(UISettings settings) {
        this.settings = settings
    }
    
    @Override
    Component getListCellRendererComponent(JList<? extends PersonaOrProfile> list, PersonaOrProfile value, 
                                           int index, boolean isSelected, boolean cellHasFocus) {
        
        JLabel rv = new JLabel()
        String text
        if (settings.personaRendererIds)
            text = "<html>" + PersonaCellRenderer.htmlize(value.getPersona()) + "</html>"
        else
            text = PersonaCellRenderer.justName(value.getPersona())

        rv.setText(text)

        if (value.getThumbnail() != null)
            rv.setIcon(value.getThumbnail())
        else
            rv.setIcon(null)

        if (value.getTitle() != null)
            rv.setToolTipText(value.getTitle())
        else
            rv.setToolTipText(trans("NO_PROFILE"))

        if (!isSelected) {
            rv.setForeground(list.getForeground())
            rv.setBackground(list.getBackground())
        } else {
            rv.setForeground(list.getSelectionForeground())
            rv.setBackground(list.getSelectionBackground())
        }
        rv
    }
}
