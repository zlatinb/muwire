package com.muwire.gui.profile

import com.muwire.core.Persona
import com.muwire.gui.HTMLSanitizer
import com.muwire.gui.UISettings

import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import java.awt.Component

import static com.muwire.gui.Translator.trans

class PersonaOrProfileCellRenderer extends DefaultTableCellRenderer {
    
    private final UISettings settings
    
    PersonaOrProfileCellRenderer(UISettings settings) {
        this.settings = settings
    }
    
    @Override
    Component getTableCellRendererComponent(JTable table, Object value,
                                            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        
        PersonaOrProfile pop = (PersonaOrProfile) value
        if (pop.getThumbnail() != null && settings.personaRendererAvatars)
            setIcon(pop.getThumbnail())
        else
            setIcon(null)
        
        if (pop.getTitle() != null) {
            if (settings.personaRendererIds)
                setToolTipText(pop.getTitle())
            else {
                String escaped = HTMLSanitizer.escape(pop.getRawTitle());
                String tooltip = "<html><body>${pop.getPersona().getHumanReadableName()}: ${escaped}</body><html>"
                setToolTipText(tooltip)
            }
        } else
            setToolTipText(trans("NO_PROFILE"))
        
        Persona persona = pop.getPersona()
        if (settings.personaRendererIds)
            setText("<html>${htmlize(persona)}</html>")
        else {
            String fullName = pop.getPersona().getHumanReadableName()
            String justName = fullName.substring(0, fullName.indexOf("@"))
            setText(justName)
        }
            
        if (isSelected) {
            setForeground(table.getSelectionForeground())
            setBackground(table.getSelectionBackground())
        } else {
            setForeground(table.getForeground())
            setBackground(table.getBackground())
        }
        this
    }

    static String htmlize(Persona persona) {
        int atIdx = persona.getHumanReadableName().indexOf("@")
        String nickname = persona.getHumanReadableName().substring(0, atIdx)
        String hashPart = persona.getHumanReadableName().substring(atIdx)
        "$nickname<font color='gray'>$hashPart</font>"
    }
}
