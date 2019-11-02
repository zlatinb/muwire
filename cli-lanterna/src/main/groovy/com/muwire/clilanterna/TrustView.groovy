package com.muwire.clilanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.table.Table
import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustSubscriptionEvent

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
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER, true, false)
        
        Panel topPanel = new Panel()
        topPanel.setLayoutManager(new GridLayout(2))
        
        trusted = new Table("Trusted Users")
        trusted.setCellSelection(false)
        trusted.setSelectAction({trustedActions()})
        trusted.setTableModel(model.modelTrusted)
        trusted.setVisibleRows(tableSize)
        topPanel.addComponent(trusted, layoutData)
        
        distrusted = new Table("Distrusted users")
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
        int selectedRow = trusted.getSelectedRow()
        def row = model.modelTrusted.getRow(selectedRow)
        Persona persona = row[0].persona
        
        Window prompt = new BasicWindow("Change Trust For "+persona.getHumanReadableName())
        prompt.setHints([Window.Hint.CENTERED])
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(4))
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        
        Button subscribe = new Button("Subscribe", {
            core.muOptions.trustSubscriptions.add(persona)
            saveMuSettings()
            core.eventBus.publish(new TrustSubscriptionEvent(persona : persona, subscribe : true))
            MessageDialog.showMessageDialog(textGUI, "Subscribed", "Subscribed from trust list of " + persona.getHumanReadableName(),
                MessageDialogButton.OK)
           model.refreshModels()
            
        })
        Button markNeutral = new Button("Mark Neutral", {
            core.eventBus.publish(new TrustEvent(persona : persona, level : TrustLevel.NEUTRAL))
            MessageDialog.showMessageDialog(textGUI, "Marked Neutral", persona.getHumanReadableName() + "has been marked neutral",
                 MessageDialogButton.OK)  
        })
        Button markDistrusted = new Button("Mark Distrusted", {
            core.eventBus.publish(new TrustEvent(persona : persona, level : TrustLevel.DISTRUSTED))
            MessageDialog.showMessageDialog(textGUI, "Marked Distrusted", persona.getHumanReadableName() + "has been marked distrusted",
                 MessageDialogButton.OK)
        })
        Button closeButton = new Button("Close", {prompt.close()})
        contentPanel.with {
            addComponent(subscribe, layoutData) 
            addComponent(markNeutral, layoutData)
            addComponent(markDistrusted, layoutData)
            addComponent(closeButton, layoutData)
        }
        prompt.setComponent(contentPanel)
        textGUI.addWindowAndWait(prompt)
    }
    
    private void distrustedActions() {
        int selectedRow = trusted.getSelectedRow()
        def row = model.modelDistrusted.getRow(selectedRow)
        Persona persona = row[0].persona
        
        Window prompt = new BasicWindow("Change Trust For "+persona.getHumanReadableName())
        prompt.setHints([Window.Hint.CENTERED])
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(3))
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        
        Button markNeutral = new Button("Mark Neutral", {
            core.eventBus.publish(new TrustEvent(persona : persona, level : TrustLevel.NEUTRAL))
            MessageDialog.showMessageDialog(textGUI, "Marked Neutral", persona.getHumanReadableName() + "has been marked neutral",
                 MessageDialogButton.OK)
        })
        Button markDistrusted = new Button("Mark Trusted", {
            core.eventBus.publish(new TrustEvent(persona : persona, level : TrustLevel.TRUSTED))
            MessageDialog.showMessageDialog(textGUI, "Marked Trusted", persona.getHumanReadableName() + "has been marked trusted",
                 MessageDialogButton.OK)
        })
        Button closeButton = new Button("Close", {prompt.close()})
        contentPanel.with {
            addComponent(markDistrusted, layoutData)
            addComponent(markNeutral, layoutData)
            addComponent(closeButton, layoutData)
        }
        prompt.setComponent(contentPanel)
        textGUI.addWindowAndWait(prompt)
    }
    
    private void trustListActions() {
        int selectedRow = subscriptions.getSelectedRow()
        def row = model.modelSubscriptions.getRow(selectedRow)
        
        def trustList = row[0].trustList
        Persona persona = trustList.persona
        
        Window prompt = new BasicWindow("Trust List Actions")
        prompt.setHints([Window.Hint.CENTERED])
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(4))
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        
        Button reviewButton = new Button("Review",{review(trustList)})
        Button updateButton = new Button("Update",{
            core.eventBus.publish(new TrustSubscriptionEvent(persona : persona, subscribe : true))
            MessageDialog.showMessageDialog(textGUI, "Updating...", "Trust list will update soon", MessageDialogButton.OK)
        })
        Button unsubscribeButton = new Button("Unsubscribe", {
            core.muOptions.trustSubscriptions.remove(persona)
            saveMuSettings()
            core.eventBus.publish(new TrustSubscriptionEvent(persona : persona, subscribe : false))
            MessageDialog.showMessageDialog(textGUI, "Unsubscribed", "Unsubscribed from trust list of " + persona.getHumanReadableName(),
                 MessageDialogButton.OK)
            model.refreshModels()
        })
        Button closeButton = new Button("Close", {prompt.close()})
        
        contentPanel.with { 
            addComponent(reviewButton, layoutData)
            addComponent(updateButton, layoutData)
            addComponent(unsubscribeButton, layoutData)
            addComponent(closeButton, layoutData)
        }
        prompt.setComponent(contentPanel)
        textGUI.addWindowAndWait(prompt)
    }
    
    private void review(def trustList) {
        TrustListModel model = new TrustListModel(trustList, core)
        TrustListView view = new TrustListView(model, textGUI, core, terminalSize)
        textGUI.addWindowAndWait(view)
        model.unregister()
    }
    
    private void saveMuSettings() {
        File settingsFile = new File(core.home,"MuWire.properties")
        settingsFile.withPrintWriter("UTF-8",{ core.muOptions.write(it) })
    }
}
