package com.muwire.clilanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextBox
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.muwire.core.Core
import com.muwire.core.chat.ChatCommand
import com.muwire.core.chat.ChatConnection
import com.muwire.core.chat.ChatMessageEvent
import com.muwire.core.chat.ChatServer

import net.i2p.data.DataHelper

class ChatConsoleView extends BasicWindow {
    private final TextGUI textGUI
    private final ChatConsoleModel model
    private final Core core
    
    private final LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER, true, false)
    private final LayoutData layoutDataFill = GridLayout.createLayoutData(Alignment.FILL, Alignment.FILL, true, false)
    
    private final TextBox textBox
    private final TextBox sayField
    private final TextBox roomField
    
    ChatConsoleView(Core core, ChatConsoleModel model, TextGUI textGUI, TerminalSize terminalSize) {
        super("Chat Server Console")
        this.core = core
        this.model = model
        this.textGUI = textGUI
        TextBox textBox = model.textBox == null ? new TextBox(terminalSize,"", TextBox.Style.MULTI_LINE) : model.textBox 
        this.textBox = textBox
        model.textBox = textBox
        model.start()
        TerminalSize textFieldSize = new TerminalSize((terminalSize.getColumns() / 2).toInteger(), 1)
        this.sayField = new TextBox(textFieldSize,"", TextBox.Style.SINGLE_LINE)
        this.roomField = new TextBox(textFieldSize,"__CONSOLE__", TextBox.Style.SINGLE_LINE)
        
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        contentPanel.addComponent(textBox, layoutData)
        
        Panel inputPanel = new Panel()
        inputPanel.with { 
            setLayoutManager(new GridLayout(2))
            addComponent(new Label("Say something here"), layoutData)
            addComponent(sayField, layoutDataFill)
            addComponent(new Label("In room:"), layoutData)
            addComponent(roomField, layoutDataFill)
        }
        contentPanel.addComponent(inputPanel, layoutData)
        
        Panel bottomPanel = new Panel()
        bottomPanel.setLayoutManager(new GridLayout(4))
        
        Button sayButton = new Button("Say",{say()})
        Button startButton = new Button("Start Server",{model.start()})
        Button stopButton = new Button("Stop Server", {model.stop()})
        Button closeButton = new Button("Close",{close()})    
        
        bottomPanel.with { 
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
        
        ChatCommand chatCommand
        try {
            chatCommand = new ChatCommand(command)
        } catch (Exception e) {
            chatCommand = new ChatCommand("/SAY $command")
        }
        command = chatCommand.source
        
        String room = roomField.getText()
        
        UUID uuid = UUID.randomUUID()
        long now = System.currentTimeMillis()
        
        String toAppend = DataHelper.formatTime(now) + " <" + core.me.getHumanReadableName() + "> [$room] " + command
        textBox.addLine(toAppend)
        
        byte[] sig = ChatConnection.sign(uuid, now, room, command, core.me, core.me, core.spk)
        
        def event = new ChatMessageEvent( uuid : uuid,
            payload : command,
            sender : core.me,
            host : core.me,
            room : room,
            chatTime : now,
            sig : sig
            )
        core.eventBus.publish(event)
    }
}
