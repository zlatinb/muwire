package com.muwire.gui

import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.Format

import static com.muwire.gui.Translator.trans

class SizeRenderer extends DefaultTableCellRenderer {
    private final String bShort;
    private final StringBuffer sb = new StringBuffer(32)

    SizeRenderer() {
        setHorizontalAlignment(JLabel.CENTER)
        bShort = trans("BYTES_SHORT")
    }
    @Override
    JComponent getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
        if (value == null) {
            // this is very strange, but it happens.  Probably a swing bug?
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        }
        Long l = (Long) value
        SizeFormatter.format(l,sb)
        sb.append(bShort)
        setText(sb.toString())
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
