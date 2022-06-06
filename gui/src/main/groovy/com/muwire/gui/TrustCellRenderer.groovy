package com.muwire.gui

import com.muwire.core.trust.TrustLevel

import static com.muwire.gui.Translator.trans

import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import java.awt.Component

class TrustCellRenderer extends DefaultTableCellRenderer {
    
    private final Icon trusted, neutral, distrusted
    
    TrustCellRenderer() {
        trusted = new ImageIcon(getClass().getClassLoader().getResource("trusted.png"))
        neutral = new ImageIcon(getClass().getClassLoader().getResource("neutral.png"))
        distrusted = new ImageIcon(getClass().getClassLoader().getResource("distrusted.png"))
    }

    @Override
    Component getTableCellRendererComponent(JTable table, Object value,
                                            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        TrustLevel level = (TrustLevel) value
        switch(value) {
            case TrustLevel.TRUSTED: setIcon(trusted); break;
            case TrustLevel.NEUTRAL: setIcon(neutral); break;
            case TrustLevel.DISTRUSTED: setIcon(distrusted); break;
        }
        
        setText("")
        setToolTipText(trans(level.name()))
        
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
