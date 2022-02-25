package com.muwire.gui

import com.muwire.core.InfoHash
import com.muwire.core.search.UIResultEvent

import javax.swing.ImageIcon
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import java.awt.Component
import java.util.function.Predicate

class ResultNameTableCellRenderer extends DefaultTableCellRenderer {
    
    private final ImageIcon sharedIcon, downloadingIcon
    private final Predicate<InfoHash> sharedPredicate, downloadingPredicate;
    private final boolean fullPath
    
    ResultNameTableCellRenderer(Predicate<InfoHash> sharedPredicate, 
                                Predicate<InfoHash> downloadingPredicate,
                                boolean fullPath) {
        this.sharedPredicate = sharedPredicate
        this.downloadingPredicate = downloadingPredicate
        sharedIcon = new ImageIcon((URL) ResultNameTableCellRenderer.class.getResource("/yes.png"))
        downloadingIcon = new ImageIcon((URL) ResultNameTableCellRenderer.class.getResource("/down_arrow.png"))
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
        else if (downloadingPredicate.test(event.infohash))
            setIcon(downloadingIcon)
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
