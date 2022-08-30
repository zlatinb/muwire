package com.muwire.gui

import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

import net.i2p.data.DataHelper
import static com.muwire.gui.Translator.trans

class DateRenderer extends DefaultTableCellRenderer {
    DateRenderer(){
        setHorizontalAlignment(CENTER)
    }
    
    JComponent getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (value == null)
            return this
        Long l = (Long) value
        String formatted
        if (l == 0)
            formatted = trans("NEVER")
        else
            formatted = DataHelper.formatTime(l)
        setText(formatted)
        if (isSelected) {
            setForeground(table.getSelectionForeground())
            setBackground(table.getSelectionBackground())
        } else {
            setForeground(table.getForeground())
            setBackground(table.getBackground())
        }
        this
    }
}
