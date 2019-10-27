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
        
        table = new Table("Name","Progress","Downloader","Remote Pieces","Speed")
        table.setCellSelection(false)
        table.setTableModel(model.model)
        table.setVisibleRows(terminalSize.getRows())
        contentPanel.addComponent(table, layoutData)

        Panel buttonsPanel = new Panel()
        buttonsPanel.setLayoutManager(new GridLayout(2))
        
        Button clearDoneButton = new Button("Clear Finished",{
            model.uploaders.removeAll { it.finished }
        })        
        Button closeButton = new Button("Close",{close()})
        
        buttonsPanel.addComponent(clearDoneButton, layoutData)
        buttonsPanel.addComponent(closeButton, layoutData)
        
        contentPanel.addComponent(buttonsPanel, layoutData)
        
        setComponent(contentPanel)
        closeButton.takeFocus()
    }
}
