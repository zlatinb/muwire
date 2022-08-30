package com.muwire.gui

import com.muwire.core.Constants
import com.muwire.core.files.FileHasher
import com.muwire.gui.profile.ProfileConstants
import net.i2p.data.DataHelper

import static com.muwire.gui.Translator.trans

import javax.swing.*
import javax.swing.table.TableColumn
import java.awt.*

class TableUtil {
    
    static void packColumns(JTable table, Set<Integer> exclude) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (exclude.contains(i))
                continue
            TableColumn column = table.getColumnModel().getColumn(i)
            String value = (String) column.getHeaderValue()
            int fixedWidth = stringWidth(table, value) + 30
            column.setMinWidth(fixedWidth)
            column.setMaxWidth(fixedWidth)
        }
    }
    
    static void sizeColumn(JTable table, int index) {
        StringBuffer buf = new StringBuffer()
        SizeFormatter.format(FileHasher.MAX_SIZE - 1, buf)
        String str = buf.toString() + trans("BYTES_SHORT").length()
        int columnSize = stringWidth(table, str)
        fixedColumnSize(table, index, columnSize + 30)
    }
    
    static void enumColumn(JTable table, int index, Class<? extends Enum> clazz) {
        int strLen = 0
        Set<Enum> set = EnumSet.allOf(clazz)
        for (Enum anEnum : set) {
            try {
                strLen = Math.max(strLen, stringWidth(table, trans(anEnum.name())))
            } catch (MissingResourceException notTranslated) {}
        }
        fixedColumnSize(table, index, strLen + 30)
    }
    
    static void speedColumn(JTable table, int index) {
        StringBuffer buf = new StringBuffer()
        SizeFormatter.format(FileHasher.MAX_SIZE - 1, buf)
        String str = buf.toString() + trans("B_SEC").length()
        int columnSize = stringWidth(table, str)
        fixedColumnSize(table, index, columnSize + 30)
    }
    
    static void dateColumn(JTable table, int index) {
        int len = stringWidth(table, trans("NEVER"))
        long now = System.currentTimeMillis()
        String formatted = DataHelper.formatTime(now)
        len = Math.max(len, stringWidth(table, formatted))
        fixedColumnSize(table, index, len + 60)
    }
    
    static void nicknameColumn(JTable table, int index) {
        String tmp = "A"
        Constants.MAX_NICKNAME_LENGTH.times {tmp += "A"}
        tmp += "@"
        32.times {tmp += "a"}
        int size = stringWidth(table, tmp) + ProfileConstants.MAX_THUMBNAIL_SIZE + 30
        TableColumn column = table.getColumnModel().getColumn(index)
        column.setMaxWidth(size)
        column.setPreferredWidth((int)(size / 2))
    }
    
    static void filesColumn(JTable table, int index) {
        String million = String.valueOf(1000000)
        int size = stringWidth(table, million)
        fixedColumnSize(table, index, size + 30)
    }
    
    private static void fixedColumnSize(JTable table, int index, int columnSize) {
        TableColumn column = table.getColumnModel().getColumn(index)
        column.setMinWidth(columnSize)
        column.setMaxWidth(columnSize)
    }
    
    static int stringWidth(Component component, String string) {
        FontMetrics fontMetrics = component.getFontMetrics(component.getFont())
        fontMetrics.getStringBounds(string, component.getGraphics()).getWidth()
    }
}
