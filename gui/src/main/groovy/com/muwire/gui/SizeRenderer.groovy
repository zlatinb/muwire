package com.muwire.gui

import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

import static com.muwire.gui.Translator.trans

import net.i2p.data.DataHelper

class SizeRenderer extends DefaultTableCellRenderer {
    SizeRenderer() {
        setHorizontalAlignment(JLabel.CENTER)
    }
    @Override
    JComponent getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
        if (value == null) {
            // this is very strange, but it happens.  Probably a swing bug?
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        }
        Long l = (Long) value
        String formatted = DataHelper.formatSize2Decimal(l, false) + trans("BYTES_SHORT")
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
