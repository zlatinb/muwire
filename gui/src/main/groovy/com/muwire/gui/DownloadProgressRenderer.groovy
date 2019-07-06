package com.muwire.gui

import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

import com.muwire.core.download.Downloader

import net.i2p.data.DataHelper

class DownloadProgressRenderer extends DefaultTableCellRenderer {
    DownloadProgressRenderer() {
        setHorizontalAlignment(JLabel.CENTER)
    }
    
    @Override
    JComponent getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
        Downloader d = (Downloader) value
        int pieces = d.nPieces
        int done = d.donePieces()
        int percent = -1
        if (pieces != 0)
            percent = (int)(done * 100.0 / pieces)
        long size = d.pieceSize * pieces
        String totalSize = DataHelper.formatSize2Decimal(size, false) + "B"
        setText(String.format("%2d", percent) + "% of ${totalSize} ($done/$pieces pcs)".toString())
        
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
