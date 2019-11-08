package com.muwire.gui

import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

import net.i2p.data.DataHelper

class DateRenderer extends DefaultTableCellRenderer {
    DateRenderer(){
        setHorizontalAlignment(JLabel.CENTER)
    }
    
    JComponent getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
        Long l = (Long) value
        String formatted = DataHelper.formatTime(l)
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
