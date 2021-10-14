package com.muwire.gui

import com.muwire.core.InfoHash
import com.muwire.core.search.UIResultEvent

import javax.swing.ImageIcon
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import java.awt.Component
import java.util.function.Predicate

class ResultNameTableCellRenderer extends DefaultTableCellRenderer {
    
    private final ImageIcon sharedIcon
    private final Predicate<InfoHash> sharedPredicate
    private final boolean fullPath
    
    ResultNameTableCellRenderer(Predicate<InfoHash> sharedPredicate, boolean fullPath) {
        this.sharedPredicate = sharedPredicate
        sharedIcon = new ImageIcon((URL) ResultNameTableCellRenderer.class.getResource("/yes.png"))
        this.fullPath = fullPath
    }

    @Override
    Component getTableCellRendererComponent(JTable table, Object value, 
                                            boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        UIResultEvent event = (UIResultEvent) value
        setText(HTMLSanitizer.sanitize(fullPath ? event.getFullPath() : event.name))
        if (sharedPredicate.test(event.infohash))
            setIcon(sharedIcon)
        else
            setIcon(null)
        setToolTipText(HTMLSanitizer.sanitize(event.getFullPath()))
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
