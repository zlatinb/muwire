package com.muwire.clilanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.table.Table

import com.muwire.core.Core

class SearchView extends BasicWindow {
    private final Core core
    private final SearchModel model
    private final Table table
    private final TextGUI textGUI
    private final TerminalSize terminalSize
    
    SearchView(SearchModel model, Core core, TextGUI textGUI, TerminalSize terminalSize) {
        super(model.query)
        this.core = core
        this.model = model
        this.textGUI = textGUI
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
    
        table = new Table("Sender","Results","Browse","Trust") 
        table.setCellSelection(false)
        table.setSelectAction({rowSelected()})
        table.setTableModel(model.model)   
        table.setVisibleRows(terminalSize.getRows())
        contentPanel.addComponent(table, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        
        Button closeButton = new Button("Close", {
            model.unregister()
            close()
        })
        contentPanel.addComponent(closeButton, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        
        setComponent(contentPanel)
        closeButton.takeFocus()
    }
    
    private void rowSelected() {
        int selectedRow = table.getSelectedRow()
        def rows = model.model.getRow(selectedRow)
        boolean browse = Boolean.parseBoolean(rows[2])
        if (browse) {
            Window prompt = new BasicWindow("Show Or Browse "+rows[0]+"?")
            prompt.setHints([Window.Hint.CENTERED])
            Panel contentPanel = new Panel()
            contentPanel.setLayoutManager(new GridLayout(3))
            Button showResults = new Button("Show Results", {
                showResults(rows[0])
            })
            Button browseHost = new Button("Browse Host", {})
            Button closePrompt = new Button("Close", {prompt.close()})
            contentPanel.addComponent(showResults, , GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
            contentPanel.addComponent(browseHost, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
            contentPanel.addComponent(closePrompt, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
            prompt.setComponent(contentPanel)
            showResults.takeFocus()
            textGUI.addWindowAndWait(prompt)
        } else {
            showResults(rows[0])    
        }
    }
    
    private void showResults(String personaName) {
        def results = model.resultsPerSender.get(personaName)
        ResultsModel resultsModel = new ResultsModel(results)
        ResultsView resultsView = new ResultsView(resultsModel, core, textGUI, terminalSize)
        textGUI.addWindowAndWait(resultsView)
    }
}
