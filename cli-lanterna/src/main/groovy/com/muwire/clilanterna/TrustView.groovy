package com.muwire.clilanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.table.Table
import com.muwire.core.Core

class TrustView extends BasicWindow {
    private final TrustModel model
    private final TextGUI textGUI
    private final Core core
    private final TerminalSize terminalSize
    private final Table trusted, distrusted, subscriptions
    
    TrustView(TrustModel model, TextGUI textGUI, Core core, TerminalSize terminalSize) {
        this.model = model
        this.textGUI = textGUI
        this.core = core
        this.terminalSize = terminalSize
        
        int tableSize = (terminalSize.getRows() / 2 - 10).toInteger()
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        
        Panel topPanel = new Panel()
        topPanel.setLayoutManager(new GridLayout(2))
        
        trusted = new Table("Trusted Users")
        trusted.setCellSelection(false)
        trusted.setSelectAction({trustedActions()})
        trusted.setTableModel(model.modelTrusted)
        trusted.setVisibleRows(tableSize)
        topPanel.addComponent(trusted, layoutData)
        
        distrusted = new Table("distrusted users")
        distrusted.setCellSelection(false)
        distrusted.setSelectAction({distrustedActions()})
        distrusted.setTableModel(model.modelDistrusted)
        distrusted.setVisibleRows(tableSize)
        topPanel.addComponent(distrusted, layoutData)
        
        Panel bottomPanel = new Panel()
        bottomPanel.setLayoutManager(new GridLayout(1))
        
        Label tableName = new Label("Trust List Subscriptions")
        bottomPanel.addComponent(tableName, layoutData)
        
        subscriptions = new Table("Name","Trusted","Distrusted","Status","Last Updated")
        subscriptions.setCellSelection(false)
        subscriptions.setSelectAction({trustListActions()})
        subscriptions.setTableModel(model.modelSubscriptions)
        subscriptions.setVisibleRows(tableSize)
        bottomPanel.addComponent(subscriptions, layoutData)
        
        Button closeButton = new Button("Close", {close()})
        
        contentPanel.addComponent(topPanel, layoutData)
        contentPanel.addComponent(bottomPanel, layoutData)
        contentPanel.addComponent(closeButton, layoutData)
        
        setComponent(contentPanel)
    }
    
    private void trustedActions() {
        
    }
    
    private void distrustedActions() {
        
    }
    
    private void trustListActions() {
        
    }
}
