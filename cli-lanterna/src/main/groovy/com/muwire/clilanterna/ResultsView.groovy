package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.table.Table

import com.muwire.core.Core

class ResultsView extends BasicWindow {
    
    private final ResultsModel model
    private final TextGUI textGUI
    private final Core core
    private final Table table
    
    ResultsView(ResultsModel model, Core core, TextGUI textGUI) {
        super(model.results.results[0].sender.getHumanReadableName() + " Results")
        this.model = model
        this.core = core
        this.textGUI = textGUI
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        
        table = new Table("Name","Size","Hash","Sources","Comment")
        table.setCellSelection(false)
        table.setSelectAction({rowSelected()})
        table.setTableModel(model.model)
        contentPanel.addComponent(table, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        
        Button closeButton = new Button("Close", {close()})
        contentPanel.addComponent(closeButton, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        
        setComponent(contentPanel)
        closeButton.takeFocus()    
    }
    
    private void rowSelected() {
        
    }
}
