package com.muwire.gui

import com.muwire.core.download.Downloader

import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

import static com.muwire.gui.Translator.trans

class DownloadStateRenderer extends DefaultTableCellRenderer {
    @Override
    JComponent getTableCellRendererComponent(JTable table, Object value,
                                             boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (value == null)
            return this //TODO investigate

        Downloader.DownloadState state = (Downloader.DownloadState) value
        setText(trans(state.name()))
        
        String tooltipKey = null
        switch(state) {
            case Downloader.DownloadState.HASHLIST : tooltipKey = "TOOLTIP_DOWNLOAD_STATE_HASHLIST"; break
            case Downloader.DownloadState.FAILED : tooltipKey = "TOOLTIP_DOWNLOAD_STATE_FAILED"; break
            case Downloader.DownloadState.REJECTED : tooltipKey = "TOOLTIP_DOWNLOAD_STATE_REJECTED"; break
            case Downloader.DownloadState.HOPELESS : tooltipKey = "TOOLTIP_DOWNLOAD_STATE_HOPELESS"; break
        }
        
        if (tooltipKey != null)
            setToolTipText(trans(tooltipKey))

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
