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
    private final TerminalSize terminalSize
    
    ResultsView(ResultsModel model, Core core, TextGUI textGUI, TerminalSize terminalSize) {
        super(model.results.results[0].sender.getHumanReadableName() + " Results")
        this.model = model
        this.core = core
        this.textGUI = textGUI
        this.terminalSize = terminalSize
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        
        table = new Table("Name","Size","Hash","Sources","Comment","Certificates")
        table.setCellSelection(false)
        table.setSelectAction({rowSelected()})
        table.setTableModel(model.model)
        table.setVisibleRows(terminalSize.getRows())
        contentPanel.addComponent(table, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER, true, false))

        Panel buttonsPanel = new Panel()
        buttonsPanel.setLayoutManager(new GridLayout(2))
        Button sortButton = new Button("Sort...",{sort()})
        buttonsPanel.addComponent(sortButton)        
        Button closeButton = new Button("Close", {close()})
        buttonsPanel.addComponent(closeButton)
        contentPanel.addComponent(buttonsPanel, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER, true, false))
        
        setComponent(contentPanel)
        closeButton.takeFocus()    
    }
    
    private void rowSelected() {
        int selectedRow = table.getSelectedRow()
        def rows = model.model.getRow(selectedRow)
        boolean comment = Boolean.parseBoolean(rows[4])
        boolean certificates = rows[5] > 0
        if (comment || certificates) {
            LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
            
            Window prompt = new BasicWindow("Download Or View Comment/Certificates")
            prompt.setHints([Window.Hint.CENTERED])
            Panel contentPanel = new Panel()
            contentPanel.setLayoutManager(new GridLayout(4))
            Button downloadButton = new Button("Download", {download(rows[2])})
            contentPanel.addComponent(downloadButton, layoutData)
            
            
            if (comment) { 
                Button viewButton = new Button("View Comment", {viewComment(rows[2])})
                contentPanel.addComponent(viewButton, layoutData)
            }
            if (certificates) {
                Button certsButton = new Button("View Certificates", {viewCertificates(rows[2])})
                contentPanel.addComponent(certsButton, layoutData)
            }
                
            Button closeButton = new Button("Cancel", {prompt.close()})
            
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
        UIResultEvent result = model.rootToResult[infohash]
        ViewCommentView view = new ViewCommentView(result.comment, result.name, terminalSize)
        textGUI.addWindowAndWait(view)
    }
    
    private void viewCertificates(String infohash) {
        UIResultEvent result = model.rootToResult[infohash]
        ViewCertificatesModel model = new ViewCertificatesModel(result, core, textGUI.getGUIThread())
        ViewCertificatesView view = new ViewCertificatesView(model, textGUI, core, terminalSize)
        textGUI.addWindowAndWait(view)
    }
    
    private void sort() {
        SortPrompt prompt = new SortPrompt(textGUI)
        SortType type = prompt.prompt()
        if (type != null)
            model.sort(type)
    }
}
