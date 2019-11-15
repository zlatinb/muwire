package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window

class SortPrompt extends BasicWindow {
    private final TextGUI textGUI
    private SortType type
    SortPrompt(TextGUI textGUI) {
        super("Select what to sort by")
        this.textGUI = textGUI
    }
    
    SortType prompt() {
        setHints([Window.Hint.CENTERED])
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(5))
        
        LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
        
        Button nameAsc = new Button("Name (ascending)",{
            type = SortType.NAME_ASC
            close()  
        })
        Button nameDesc = new Button("Name (descending)",{
            type = SortType.NAME_DESC
            close()
        })
        Button sizeAsc = new Button("Size (ascending)",{
            type = SortType.SIZE_ASC
            close()
        })
        Button sizeDesc = new Button("Size (descending)",{
            type = SortType.SIZE_DESC
            close()
        })
        Button close = new Button("Cancel",{close()})
        
        contentPanel.with { 
            addComponent(nameAsc, layoutData)
            addComponent(nameDesc, layoutData)
            addComponent(sizeAsc, layoutData)
            addComponent(sizeDesc, layoutData)
            addComponent(close, layoutData)
        }
        
        setComponent(contentPanel)
        textGUI.addWindowAndWait(this)
        type
    }
}
