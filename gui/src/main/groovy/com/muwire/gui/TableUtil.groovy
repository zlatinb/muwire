package com.muwire.gui

import com.muwire.core.files.FileHasher

import static com.muwire.gui.Translator.trans

import javax.swing.*
import javax.swing.table.TableColumn
import java.awt.*

class TableUtil {
    
    private static final int SIZE_COLUMN 
    static {
        StringBuffer buf = new StringBuffer()
        SizeFormatter.format(FileHasher.MAX_SIZE - 1, buf)
        String str = buf.toString() + trans("BYTES_SHORT").length()
        SIZE_COLUMN = stringWidth(new JLabel(), str) + 30
    }
    
    static void packColumns(JTable table, Set<Integer> exclude) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (exclude.contains(i))
                continue
            TableColumn column = table.getColumnModel().getColumn(i)
            String value = (String) column.getHeaderValue()
            int fixedWidth = stringWidth(table, value) + 10
            column.setMinWidth(fixedWidth)
            column.setMaxWidth(fixedWidth)
        }
    }
    
    static void sizeColumn(JTable table, int index) {
        TableColumn column = table.getColumnModel().getColumn(index)
        column.setMinWidth(SIZE_COLUMN)
        column.setMaxWidth(SIZE_COLUMN)
    }
    
    static int stringWidth(Component component, String string) {
        FontMetrics fontMetrics = component.getFontMetrics(component.getFont())
        fontMetrics.getStringBounds(string, component.getGraphics()).getWidth()
    }
}
