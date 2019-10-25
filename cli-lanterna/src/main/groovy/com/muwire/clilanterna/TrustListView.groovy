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
import com.muwire.core.Persona
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

class TrustListView extends BasicWindow {
    private final TrustListModel model
    private final TextGUI textGUI
    private final Core core
    private final TerminalSize terminalSize
    private final Table trusted, distrusted
    
    TrustListView(TrustListModel model, TextGUI textGUI, Core core, TerminalSize terminalSize) {
        this.model = model
        this.textGUI = textGUI
        this.core = core
        this.terminalSize = terminalSize
        
        int tableSize = terminalSize.getRows() - 10
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        
        Label nameLabel = new Label("Trust list for "+model.trustList.persona.getHumanReadableName())
        Label lastUpdatedLabel = new Label("Last updated "+new Date(model.trustList.timestamp))
        contentPanel.addComponent(nameLabel, layoutData)
        contentPanel.addComponent(lastUpdatedLabel, layoutData)
        
        
        Panel topPanel = new Panel()
        topPanel.setLayoutManager(new GridLayout(2))
        
        trusted = new Table("Trusted User","Your Trust")
        trusted.with {
            setCellSelection(false)
            setTableModel(model.trustedTableModel)
            setVisibleRows(tableSize)
        }
        trusted.setSelectAction({ actionsForUser(true) })
        topPanel.addComponent(trusted, layoutData)
        
        distrusted = new Table("Distrusted User", "Your Trust")
        distrusted.with { 
            setCellSelection(false)
            setTableModel(model.distrustedTableModel)
            setVisibleRows(tableSize)
        }
        distrusted.setSelectAction({actionsForUser(false)}) 
        topPanel.addComponent(distrusted, layoutData)

        Button closeButton = new Button("Close",{close()})
        
        contentPanel.addComponent(topPanel, layoutData)
        contentPanel.addComponent(closeButton, layoutData)
        
        setComponent(contentPanel)        
    }

    private void actionsForUser(boolean trustedUser) {
        def table = trustedUser ? trusted : distrusted
        def model = trustedUser ? model.trustedTableModel : model.distrustedTableModel
        
        int selectedRow = table.getSelectedRow()
        def row = model.getRow(selectedRow)
        
        Persona persona = row[0].persona
        
        Window prompt = new BasicWindow("Actions for "+persona.getHumanReadableName())
        prompt.setHints([Window.Hint.CENTERED])
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(4))
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        
        Button trustButton = new Button("Trust",{
            core.eventBus.publish(new TrustEvent(persona : persona, level : TrustLevel.TRUSTED))
            MessageDialog.showMessageDialog(textGUI, "Marked Trusted", persona.getHumanReadableName() + "has been marked trusted",
                MessageDialogButton.OK)
        })
        Button neutralButton = new Button("Neutral",{
            core.eventBus.publish(new TrustEvent(persona : persona, level : TrustLevel.NEUTRAL))
            MessageDialog.showMessageDialog(textGUI, "Marked Neutral", persona.getHumanReadableName() + "has been marked neutral",
                MessageDialogButton.OK)
        })
        Button distrustButton = new Button("Distrust",{
            core.eventBus.publish(new TrustEvent(persona : persona, level : TrustLevel.DISTRUSTED))
            MessageDialog.showMessageDialog(textGUI, "Marked Distrusted", persona.getHumanReadableName() + "has been marked distrusted",
                MessageDialogButton.OK)
        })
        Button closeButton = new Button("Close",{prompt.close()})
        
        contentPanel.with { 
            addComponent(trustButton,layoutData)
            addComponent(neutralButton, layoutData)
            addComponent(distrustButton, layoutData)
            addComponent(closeButton, layoutData)
        }
        prompt.setComponent(contentPanel)
        textGUI.addWindowAndWait(prompt)
    }    
}
