package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.table.Table

import com.muwire.core.Core

class SearchView extends BasicWindow {
    private final Core core
    private final SearchModel model
    private final Table table
    
    SearchView(SearchModel model, Core core) {
        super(model.query)
        this.core = core
        this.model = model
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        
        Button closeButton = new Button("Close", {
            model.unregister()
            close()
        })
        contentPanel.addComponent(closeButton)
    
        table = new Table("Sender","Results","Browse","Trust") 
        table.setCellSelection(false)
        table.setTableModel(model.model)   
        contentPanel.addComponent(table)
        
        setComponent(contentPanel)
        closeButton.takeFocus()
    }
}
