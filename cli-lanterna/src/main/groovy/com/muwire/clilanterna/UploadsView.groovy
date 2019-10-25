package com.muwire.clilanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.table.Table

class UploadsView extends BasicWindow {
    private final UploadsModel model
    private final Table table
    
    UploadsView(UploadsModel model, TerminalSize terminalSize) {
        this.model = model
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER, true, false)
        
        table = new Table("Name","Progress","Downloader","Remote Pieces")
        table.setCellSelection(false)
        table.setTableModel(model.model)
        table.setVisibleRows(terminalSize.getRows())
        contentPanel.addComponent(table, layoutData)
        
        Button closeButton = new Button("Close",{close()})
        contentPanel.addComponent(closeButton, layoutData)
        
        setComponent(contentPanel)
        closeButton.takeFocus()
    }
}
