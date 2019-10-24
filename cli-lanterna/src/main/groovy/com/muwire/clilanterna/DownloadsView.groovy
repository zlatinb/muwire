package com.muwire.clilanterna

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
import com.muwire.core.download.Downloader
import com.muwire.core.download.UIDownloadCancelledEvent

class DownloadsView extends BasicWindow {
    private final Core core
    private final DownloadsModel model
    private final TextGUI textGUI
    private final Table table
    
    DownloadsView(Core core, DownloadsModel model, TextGUI textGUI) {
        this.core = core
        this.model = model
        this.textGUI = textGUI
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        table = new Table("Name","Status","Progress","Speed","ETA")
        table.setCellSelection(false)
        table.setSelectAction({rowSelected()})
        table.setTableModel(model.model)
        contentPanel.addComponent(table, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        
        Button closeButton = new Button("Close",{close()})
        contentPanel.addComponent(closeButton, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        
        setComponent(contentPanel)
        closeButton.takeFocus()
    }
    
    private void rowSelected() {
        int selectedRow = table.getSelectedRow()
        def row = model.model.getRow(selectedRow)
        Downloader downloader = row[0].downloader
        
        Window prompt = new BasicWindow("Kill Download?")
        prompt.setHints([Window.Hint.CENTERED])
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(2))
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        
        Button killDownload = new Button("Kill Download", {
            downloader.cancel()
            core.eventBus.publish(new UIDownloadCancelledEvent(downloader : downloader))
            MessageDialog.showMessageDialog(textGUI, "Download Killed", downloader.file.getName()+ " has been killed", MessageDialogButton.OK)
        })
        Button close = new Button("Close", {
            prompt.close()  
        })
        
        contentPanel.addComponent(killDownload,layoutData)
        contentPanel.addComponent(close, layoutData)
        prompt.setComponent(contentPanel)
        close.takeFocus()
        textGUI.addWindowAndWait(prompt)
    }
}
