package com.muwire.gui

import com.muwire.core.SharedFile

import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import java.awt.Component

class SharedFileNameRenderer extends DefaultTableCellRenderer {
    @Override
    Component getTableCellRendererComponent(JTable table, Object value,
                                            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        SharedFile sf = (SharedFile) value
        String fullPath = HTMLSanitizer.sanitize(sf.getCachedPath())
        setText(fullPath)
        setToolTipText(fullPath)
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
