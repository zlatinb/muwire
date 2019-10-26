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
import com.googlecode.lanterna.gui2.dialogs.FileDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog
import com.googlecode.lanterna.gui2.table.Table
import com.muwire.core.Core
import com.muwire.core.SharedFile
import com.muwire.core.files.DirectoryUnsharedEvent
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.files.UIPersistFilesEvent

class FilesView extends BasicWindow {
    private final FilesModel model
    private final TextGUI textGUI
    private final Core core
    private final Table table
    private final TerminalSize terminalSize
    
    FilesView(FilesModel model, TextGUI textGUI, Core core, TerminalSize terminalSize) {
        super("Shared Files")
        this.model = model
        this.core = core
        this.textGUI = textGUI
        this.terminalSize = terminalSize
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER, true, false)
        
        table = new Table("Name","Size","Comment","Search Hits","Downloaders")
        table.setCellSelection(false)
        table.setTableModel(model.model)
        table.setSelectAction({rowSelected()})
        table.setVisibleRows(terminalSize.getRows())
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
        contentPanel.setLayoutManager(new GridLayout(3))
        
        Button unshareButton = new Button("Unshare", {
            core.eventBus.publish(new FileUnsharedEvent(unsharedFile : sf))
            core.eventBus.publish(new UIPersistFilesEvent())
            MessageDialog.showMessageDialog(textGUI, "File Unshared", "Unshared "+sf.getFile().getName(), MessageDialogButton.OK)
        } )
        Button addCommentButton = new Button("Add Comment", {
            AddCommentView view = new AddCommentView(textGUI, core, sf, terminalSize)
            textGUI.addWindowAndWait(view)
        })
        Button closeButton = new Button("Close", {prompt.close()})
        
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        contentPanel.addComponent(unshareButton, layoutData)
        contentPanel.addComponent(addCommentButton, layoutData)
        contentPanel.addComponent(closeButton, layoutData)
        
        prompt.setComponent(contentPanel)
        textGUI.addWindowAndWait(prompt)
    }
    
    
    private void shareFile() {
        TerminalSize terminalSize = new TerminalSize(terminalSize.getColumns() - 10, terminalSize.getRows() - 10)
        FileDialog fileDialog = new FileDialog("Share File", "Select a file to share", "Share", terminalSize, false, null)
        File f = fileDialog.showDialog(textGUI)
        f = f.getCanonicalFile()
        core.eventBus.publish(new FileSharedEvent(file : f))
        MessageDialog.showMessageDialog(textGUI, "File Shared", f.getName()+" has been shared", MessageDialogButton.OK)
    }
    
    private void shareDirectory() {
        String directoryName = TextInputDialog.showDialog(textGUI, "Share a directory", "Enter the directory to share", "")
        if (directoryName == null)
            return
        File directory = new File(directoryName)
        directory = directory.getCanonicalFile()
        core.eventBus.publish(new FileSharedEvent(file : directory))
        MessageDialog.showMessageDialog(textGUI, "Directory Shared", directory.getName()+" has been shared", MessageDialogButton.OK)
    }
    
    private void unshareDirectory() {
        String directoryName = TextInputDialog.showDialog(textGUI, "Unshare a directory", "Enter the directory to unshare", "")
        if (directoryName == null)
            return
        File directory = new File(directoryName)
        directory = directory.getCanonicalFile()
        core.eventBus.publish(new DirectoryUnsharedEvent(directory : directory))
        MessageDialog.showMessageDialog(textGUI, "Directory Unshared", directory.getName()+" has been unshared", MessageDialogButton.OK)
    }
}
