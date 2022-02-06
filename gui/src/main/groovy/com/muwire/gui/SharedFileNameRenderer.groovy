package com.muwire.gui

import com.muwire.core.SharedFile

import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import java.awt.Component
import java.nio.file.Path

class SharedFileNameRenderer extends DefaultTableCellRenderer {
    
    private final UISettings settings
    
    SharedFileNameRenderer(UISettings settings) {
        this.settings = settings
    }
    
    @Override
    Component getTableCellRendererComponent(JTable table, Object value,
                                            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        SharedFile sf = (SharedFile) value
        String fullPath = HTMLSanitizer.sanitize(sf.getCachedPath())
        setToolTipText(fullPath)
        if (settings.showUnsharedPaths || sf.getPathToSharedParent() == null) {
            setText(fullPath)
        } else {
            setText(HTMLSanitizer.sanitize(sf.getCachedVisiblePath()))
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
}
