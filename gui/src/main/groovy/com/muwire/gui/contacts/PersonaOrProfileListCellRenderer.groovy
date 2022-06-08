package com.muwire.gui.contacts

import com.muwire.gui.PersonaCellRenderer
import com.muwire.gui.UISettings
import com.muwire.gui.profile.PersonaOrProfile

import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer
import java.awt.Component

import static com.muwire.gui.Translator.trans

class PersonaOrProfileListCellRenderer extends DefaultListCellRenderer {

    private final UISettings settings
    
    PersonaOrProfileListCellRenderer(UISettings settings) {
        this.settings = settings
    }
    
    @Override
    Component getListCellRendererComponent(JList<?> list, Object v, 
                                           int index, boolean isSelected, boolean cellHasFocus) {
        if (v == null)
            return this
        
        super.getListCellRendererComponent(list, v, index, isSelected, cellHasFocus)
        PersonaOrProfile value = (PersonaOrProfile)v
        
        if (value.getPersona() == null) 
            return this
        
        String text
        if (settings.personaRendererIds)
            text = "<html>" + PersonaCellRenderer.htmlize(value.getPersona()) + "</html>"
        else
            text = PersonaCellRenderer.justName(value.getPersona())

        setText(text)

        if (value.getThumbnail() != null)
            setIcon(value.getThumbnail())
        else
            setIcon(null)

        if (value.getTitle() != null)
            setToolTipText(value.getTitle())
        else
            setToolTipText(trans("NO_PROFILE"))

        if (!isSelected) {
            setForeground(list.getForeground())
            setBackground(list.getBackground())
        } else {
            setForeground(list.getSelectionForeground())
            setBackground(list.getSelectionBackground())
        }
        this
    }
}
