package com.muwire.gui

import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

import net.i2p.data.DataHelper

class SizeRenderer extends DefaultTableCellRenderer {
    SizeRenderer() {
        setHorizontalAlignment(JLabel.CENTER)
    }
    @Override
    JComponent getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
        Long l = (Long) value
        String formatted = DataHelper.formatSize2Decimal(l, false)+"B"
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
