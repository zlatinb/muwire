package com.muwire.clilanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import com.googlecode.lanterna.gui2.table.Table

import com.muwire.core.Core
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.search.UIResultEvent

class ResultsView extends BasicWindow {
    
    private final ResultsModel model
    private final TextGUI textGUI
    private final Core core
    private final Table table
    
    ResultsView(ResultsModel model, Core core, TextGUI textGUI, TerminalSize terminalSize) {
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
        table.setSize(terminalSize)
        contentPanel.addComponent(table, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        
        Button closeButton = new Button("Close", {close()})
        contentPanel.addComponent(closeButton, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        
        setComponent(contentPanel)
        closeButton.takeFocus()    
    }
    
    private void rowSelected() {
        int selectedRow = table.getSelectedRow()
        def rows = model.model.getRow(selectedRow)
        boolean comment = Boolean.parseBoolean(rows[4])
        if (comment) {
            Window prompt = new BasicWindow("Download Or View Comment")
            prompt.setHints([Window.Hint.CENTERED])
            Panel contentPanel = new Panel()
            contentPanel.setLayoutManager(new GridLayout(3))
            Button downloadButton = new Button("Download", {download(rows[2])})
            Button viewButton = new Button("View Comment", {viewComment(rows[2])})
            Button closeButton = new Button("Cancel", {prompt.close()})
            
            LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
            contentPanel.addComponent(downloadButton, layoutData)
            contentPanel.addComponent(viewButton, layoutData)
            contentPanel.addComponent(closeButton, layoutData)
            prompt.setComponent(contentPanel)
            downloadButton.takeFocus()
            textGUI.addWindowAndWait(prompt)    
        } else {
            download(rows[2])
        }

    }
    
    private void download(String infohash) {
        UIResultEvent result = model.rootToResult[infohash]
        def file = new File(core.muOptions.downloadLocation, result.name)
        
        core.eventBus.publish(new UIDownloadEvent(result : [result], sources : result.sources,
            target : file, sequential : false))
        MessageDialog.showMessageDialog(textGUI, "Download Started", "Started download of "+result.name, MessageDialogButton.OK)
    }
    
    private void viewComment(String infohash) {
        
    }
}
