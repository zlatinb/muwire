package com.muwire.clilanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextBox
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.muwire.core.Core
import com.muwire.core.chat.ChatConnection
import com.muwire.core.chat.ChatMessageEvent
import com.muwire.core.chat.ChatServer

import net.i2p.data.DataHelper

class ChatConsoleView extends BasicWindow {
    private final TextGUI textGUI
    private final ChatConsoleModel model
    private final Core core
    
    private final LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER, true, false)
    
    private final TextBox textBox
    private final TextBox sayField
    
    ChatConsoleView(Core core, ChatConsoleModel model, TextGUI textGUI, TerminalSize terminalSize) {
        super("Chat Server Console")
        this.core = core
        this.model = model
        this.textGUI = textGUI
        this.textBox = new TextBox(terminalSize,"", TextBox.Style.MULTI_LINE)
        model.textBox = textBox
        model.start()
        this.sayField = new TextBox("", TextBox.Style.SINGLE_LINE)
        
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        contentPanel.addComponent(textBox, layoutData)
        
        Panel bottomPanel = new Panel()
        bottomPanel.setLayoutManager(new GridLayout(5))
        
        Button sayButton = new Button("Say",{say()})
        Button startButton = new Button("Start Server",{model.start()})
        Button stopButton = new Button("Stop Server", {model.stop()})
        Button closeButton = new Button("Close",{close()})    
        
        bottomPanel.with { 
            addComponent(sayField,layoutData)
            addComponent(sayButton, layoutData)
            addComponent(startButton, layoutData)
            addComponent(stopButton, layoutData)
            addComponent(closeButton, layoutData)
        }    
        contentPanel.addComponent(bottomPanel, layoutData)
        setComponent(contentPanel)
    }
    
    private void say() {
        String command = sayField.getText()
        sayField.setText("")
        
        UUID uuid = UUID.randomUUID()
        long now = System.currentTimeMillis()
        
        String toAppend = DataHelper.formatTime(now) + " <" + core.me.getHumanReadableName() + "> [Console] " + command
        textBox.addLine(toAppend)
        
        byte[] sig = ChatConnection.sign(uuid, now, ChatServer.CONSOLE, command, core.me, core.me, core.spk)
        
        def event = new ChatMessageEvent( uuid : uuid,
            payload : command,
            sender : core.me,
            host : core.me,
            room : ChatServer.CONSOLE,
            chatTime : now,
            sig : sig
            )
        core.eventBus.publish(event)
    }
}
