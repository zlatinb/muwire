package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.TextBox
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.Panel
import com.muwire.core.Core
import com.muwire.core.SharedFile
import com.muwire.core.files.UICommentEvent
import com.muwire.core.util.DataUtil

import net.i2p.data.Base64

class AddCommentView extends BasicWindow {
    private final TextGUI textGUI
    private final Core core
    private final TextBox textBox
    private final LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
      
    AddCommentView(TextGUI textGUI, Core core, SharedFile sharedFile, TerminalSize terminalSize) {
        super("Add Comment To "+sharedFile.getFile().getName())
        this.textGUI = textGUI
        this.core = core
        
        setHints([Window.Hint.CENTERED])

        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
                
        String oldComment = sharedFile.getComment()
        if (oldComment == null)
            oldComment = ""
        else
            oldComment = DataUtil.readi18nString(Base64.decode(oldComment))
            
        TerminalSize boxSize = new TerminalSize((terminalSize.getColumns() / 2).toInteger(), (terminalSize.getRows() / 2).toInteger())
        textBox = new TextBox(boxSize,oldComment,TextBox.Style.MULTI_LINE)    
        contentPanel.addComponent(textBox, layoutData)
        
        Panel buttonsPanel = new Panel()
        buttonsPanel.setLayoutManager(new GridLayout(2))
        contentPanel.addComponent(buttonsPanel, layoutData)
        
        Button saveButton = new Button("Save", {
            String newComment = textBox.getText()
            newComment = Base64.encode(DataUtil.encodei18nString(newComment))
            sharedFile.setComment(newComment)
            core.eventBus.publish(new UICommentEvent(sharedFile : sharedFile, oldComment : oldComment))
            close()
        })
        Button cancelButton = new Button("Cancel", {close()})
        
        buttonsPanel.addComponent(saveButton, layoutData)
        buttonsPanel.addComponent(cancelButton, layoutData)
        
        setComponent(contentPanel)
    }
}
