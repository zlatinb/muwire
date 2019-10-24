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
import com.muwire.core.SharedFile
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.files.UIPersistFilesEvent

class FilesView extends BasicWindow {
    private final FilesModel model
    private final TextGUI textGUI
    private final Core core
    private final Table table
    
    FilesView(FilesModel model, TextGUI textGUI, Core core, TerminalSize terminalSize) {
        super("Shared Files")
        this.model = model
        this.core = core
        this.textGUI = textGUI
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        
        table = new Table("Name","Size","Comment")
        table.setCellSelection(false)
        table.setTableModel(model.model)
        table.setSelectAction({rowSelected()})
        table.setSize(terminalSize)
        contentPanel.addComponent(table, layoutData)
        
        Panel buttonsPanel = new Panel()
        buttonsPanel.setLayoutManager(new GridLayout(4))
        
        Button shareFile = new Button("Share File", {shareFile()})
        Button shareDirectory = new Button("Share Directory", {shareDirectory()})
        Button unshareDirectory = new Button("Unshare Directory",{unshareDirectory()})
        Button close = new Button("Close", {close()})
        
        buttonsPanel.with { 
            addComponent(shareFile, layoutData)
            addComponent(shareDirectory, layoutData)
            addComponent(unshareDirectory, layoutData)
            addComponent(close, layoutData)
        }
        
        contentPanel.addComponent(buttonsPanel, layoutData)
        setComponent(contentPanel)
        close.takeFocus()
    }
    
    private void rowSelected() {
        int selectedRow = table.getSelectedRow()
        def row = model.model.getRow(selectedRow)
        SharedFile sf = row[0].sharedFile
        
        Window prompt = new BasicWindow("Unshare or add comment to "+sf.getFile().getName()+" ?")
        prompt.setHints([Window.Hint.CENTERED])
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(2))
        
        Button unshareButton = new Button("Unshare", {
            core.eventBus.publish(new FileUnsharedEvent(unsharedFile : sf))
            core.eventBus.publish(new UIPersistFilesEvent())
            MessageDialog.showMessageDialog(textGUI, "File Unshared", "Unshared "+sf.getFile().getName(), MessageDialogButton.OK)
        } )
        Button addCommentButton = new Button("Add comment", {})
        Button closeButton = new Button("Close", {prompt.close()})
        
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        contentPanel.addComponent(unshareButton, layoutData)
        contentPanel.addComponent(addCommentButton, layoutData)
        contentPanel.addComponent(closeButton, layoutData)
        
        prompt.setComponent(contentPanel)
        textGUI.addWindowAndWait(prompt)
    }
    
    
    private void shareFile() {
        
    }
    
    private void shareDirectory() {
        
    }
    
    private void unshareDirectory() {
        
    }
}
