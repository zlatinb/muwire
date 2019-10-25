package com.muwire.clilanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextBox
import com.googlecode.lanterna.gui2.Window
import com.muwire.core.SharedFile
import com.muwire.core.search.UIResultEvent
import com.muwire.core.util.DataUtil

import net.i2p.data.Base64

class ViewCommentView extends BasicWindow {
    private final TextBox textBox
    private final LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
    
    ViewCommentView(UIResultEvent result, TerminalSize terminalSize) {
        super("View Comments For "+result.getName())
        
        setHints([Window.Hint.CENTERED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        
        TerminalSize boxSize = new TerminalSize((terminalSize.getColumns() / 2).toInteger(), (terminalSize.getRows() / 2).toInteger())
        textBox = new TextBox(boxSize, result.comment, TextBox.Style.MULTI_LINE)
        contentPanel.addComponent(textBox, layoutData)
        
        Button closeButton = new Button("Close", {close()})
        contentPanel.addComponent(closeButton, layoutData)
        
        setComponent(contentPanel)
    }
}
