package com.muwire.clilanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import com.googlecode.lanterna.gui2.table.Table
import com.muwire.core.Core
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.search.UIResultEvent


class BrowseView extends BasicWindow {
    private final BrowseModel model
    private final TextGUI textGUI
    private final Core core
    private final Table table
    private final TerminalSize terminalSize
    private final LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER, true, false) 
    
    BrowseView(BrowseModel model, TextGUI textGUI, Core core, TerminalSize terminalSize) {
        super("Browse "+model.persona.getHumanReadableName())
        this.model = model
        this.textGUI = textGUI
        this.core = core
        this.terminalSize = terminalSize
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        
        Label statusLabel = new Label("")
        Label percentageLabel = new Label("")
        model.setStatusLabel(statusLabel)
        model.setPercentageLabel(percentageLabel)
        
        Panel topPanel = new Panel()
        topPanel.setLayoutManager(new GridLayout(2))
        topPanel.addComponent(statusLabel, layoutData)
        topPanel.addComponent(percentageLabel, layoutData)
        contentPanel.addComponent(topPanel, layoutData)
        
        table = new Table("Name","Size","Hash","Comment","Certificates")
        table.with { 
            setCellSelection(false)
            setTableModel(model.model)
            setVisibleRows(terminalSize.getRows())
            setSelectAction({rowSelected()})
        }
        contentPanel.addComponent(table, layoutData)
        
        Button closeButton = new Button("Close",{
            model.unregister()
            close()
        })
        contentPanel.addComponent(closeButton, layoutData)
        setComponent(contentPanel)
        
    }
    
    private void rowSelected() {
        int selectedRow = table.getSelectedRow()
        def row = model.model.getRow(selectedRow)
        String infoHash = row[2]
        boolean comment = Boolean.parseBoolean(row[3]) 
        boolean certificates = Integer.parseInt(row[4]) > 0
        if (comment || certificates) {
            Window prompt = new BasicWindow("Download Or View Comment")
            prompt.setHints([Window.Hint.CENTERED])
            Panel contentPanel = new Panel()
            contentPanel.setLayoutManager(new GridLayout(4))
            Button downloadButton = new Button("Download", {download(infoHash)})
            Button viewButton = new Button("View Comment", {viewComment(infoHash)})
            Button viewCertificate = new Button("View Certificates",{viewCertificates(infoHash)})
            Button closeButton = new Button("Cancel", {prompt.close()})
            
            contentPanel.with { 
                addComponent(downloadButton, layoutData)
                addComponent(viewButton, layoutData)
                addComponent(closeButton, layoutData)
            }
            
            prompt.setComponent(contentPanel)
            downloadButton.takeFocus()
            textGUI.addWindowAndWait(prompt)
        } else {
            download(infoHash)
        }
    }
    
    private void download(String infoHash) {
        UIResultEvent result = model.rootToResult[infoHash]
        def file = new File(core.muOptions.downloadLocation, result.name)
        core.eventBus.publish(new UIDownloadEvent(result : [result], sources : result.sources,
            target : file, sequential : false))
        MessageDialog.showMessageDialog(textGUI, "Download started", "Started download of "+result.name, MessageDialogButton.OK)
    }
    
    private void viewComment(String infoHash) {
        UIResultEvent result = model.rootToResult[infoHash]
        ViewCommentView view = new ViewCommentView(result.comment, result.name, terminalSize)
        textGUI.addWindowAndWait(view)
    }
    
    private void viewCertificates(String infoHash) {
        UIResultEvent result = model.rootToResult[infoHash]
        ViewCertificatesModel model = new ViewCertificatesModel(result, core, textGUI.getGUIThread())
        ViewCertificatesView view = new ViewCertificatesView(model, textGUI, core, terminalSize)
        textGUI.addWindowAndWait(view)
    }
}
