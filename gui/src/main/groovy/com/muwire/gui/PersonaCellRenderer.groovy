package com.muwire.gui

import com.muwire.core.Persona

import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import java.awt.Component

class PersonaCellRenderer extends DefaultTableCellRenderer {

    @Override
    Component getTableCellRendererComponent(JTable table, Object value,
                                            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        Persona persona = (Persona) value
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
