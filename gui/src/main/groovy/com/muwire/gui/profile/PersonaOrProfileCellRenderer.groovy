package com.muwire.gui.profile

import com.muwire.core.Persona

import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import java.awt.Component

import static com.muwire.gui.Translator.trans

class PersonaOrProfileCellRenderer extends DefaultTableCellRenderer {
    @Override
    Component getTableCellRendererComponent(JTable table, Object value,
                                            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        
        PersonaOrProfile pop = (PersonaOrProfile) value
        if (pop.getThumbnail() != null)
            setIcon(pop.getThumbnail())
        else
            setIcon(null)
        
        if (pop.getTitle() != null)
            setToolTipText(pop.getTitle())
        else
            setToolTipText(trans("NO_PROFILE"))
        
        Persona persona = pop.getPersona()
        setText("<html>${htmlize(persona)}</html>")
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
