package com.muwire.gui

import com.muwire.core.Persona

import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import java.awt.Component

class PersonaCellRenderer extends DefaultTableCellRenderer {
    
    private final UISettings settings
    
    PersonaCellRenderer(UISettings settings) {
        this.settings = settings
    }

    @Override
    Component getTableCellRendererComponent(JTable table, Object value,
                                            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (value == null)
            return this // TODO: investigate how this can possibly happen
        Persona persona = (Persona) value
        if (settings.personaRendererIds)
            setText("<html>${htmlize(persona)}</html>")
        else 
            setText(justName(persona))
        
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
    
    static String justName(Persona persona) {
        String full = persona.getHumanReadableName()
        full.substring(0,full.indexOf("@"))
    }
}
