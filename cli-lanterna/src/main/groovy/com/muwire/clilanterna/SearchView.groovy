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
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import com.googlecode.lanterna.gui2.table.Table

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

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
        this.terminalSize = terminalSize
        
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
        Persona persona = rows[0].persona
        boolean browse = Boolean.parseBoolean(rows[2])
        Window prompt = new BasicWindow("Show Or Browse "+rows[0]+"?")
        prompt.setHints([Window.Hint.CENTERED])
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(6))
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        Button showResults = new Button("Show Results", {
            showResults(persona)
        })
        Button browseHost = new Button("Browse Host", {
            BrowseModel model = new BrowseModel(persona, core, textGUI.getGUIThread())
            BrowseView view = new BrowseView(model, textGUI, core, terminalSize)
            textGUI.addWindowAndWait(view)
        }) 
        Button trustHost = new Button("Trust",{
            core.eventBus.publish(new TrustEvent(persona : persona, level : TrustLevel.TRUSTED))
            MessageDialog.showMessageDialog(textGUI, "Marked Trusted", persona.getHumanReadableName() + " has been marked trusted",
                    MessageDialogButton.OK)
        })
        Button neutralHost = new Button("Neutral",{
            core.eventBus.publish(new TrustEvent(persona : persona, level : TrustLevel.NEUTRAL))
            MessageDialog.showMessageDialog(textGUI, "Marked Neutral", persona.getHumanReadableName() + " has been marked neutral",
                    MessageDialogButton.OK)
        })
        Button distrustHost = new Button("Distrust", {
            core.eventBus.publish(new TrustEvent(persona : persona, level : TrustLevel.DISTRUSTED))
            MessageDialog.showMessageDialog(textGUI, "Marked Distrusted", persona.getHumanReadableName() + " has been marked distrusted",
                    MessageDialogButton.OK)
        })
        Button closePrompt = new Button("Close", {prompt.close()})

        contentPanel.with {
            addComponent(showResults, layoutData)
            if (browse)
                addComponent(browseHost, layoutData)
            addComponent(trustHost, layoutData)
            addComponent(neutralHost, layoutData)
            addComponent(distrustHost, layoutData)
            addComponent(closePrompt, layoutData)
        }

        prompt.setComponent(contentPanel)
        showResults.takeFocus()
        textGUI.addWindowAndWait(prompt)
    }
    
    private void showResults(Persona persona) {
        def results = model.resultsPerSender.get(persona)
        ResultsModel resultsModel = new ResultsModel(results)
        ResultsView resultsView = new ResultsView(resultsModel, core, textGUI, terminalSize)
        textGUI.addWindowAndWait(resultsView)
    }
}
