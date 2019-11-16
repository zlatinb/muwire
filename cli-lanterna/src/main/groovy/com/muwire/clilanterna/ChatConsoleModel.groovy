package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.TextBox
import com.googlecode.lanterna.gui2.TextGUIThread
import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.chat.ChatConnectionEvent
import com.muwire.core.chat.ChatLink
import com.muwire.core.chat.ChatMessageEvent
import com.muwire.core.chat.UIConnectChatEvent

import net.i2p.data.DataHelper

class ChatConsoleModel {
    private final Core core
    private final TextGUIThread guiThread
    
    volatile ChatLink link
    volatile Thread poller
    volatile boolean running
    
    volatile TextBox textBox
    
    
    ChatConsoleModel(Core core, TextGUIThread guiThread) {
        this.core = core
        this.guiThread = guiThread
    }
    
    void start() {
        if (running)
            return
        running = true
        core.chatServer.start()
        core.eventBus.with { 
            register(ChatConnectionEvent.class, this)
            publish(new UIConnectChatEvent(host : core.me))
        }
    }
    
    void onChatConnectionEvent(ChatConnectionEvent e) {
        if (e.persona != core.me)
            return // can't really happen
        
        link = e.connection
        poller = new Thread({eventLoop()} as Runnable)
        poller.setDaemon(true)
        poller.start()
    }
    
    void stop() {
        if (!running)
            return
        running = false
        core.chatServer.stop()
        poller?.interrupt()
        link = null
    }
    
    private void eventLoop() {
        Thread.sleep(1000)
        while(running) {
            ChatLink link = this.link
            if (link == null || !link.isUp()) {
                Thread.sleep(100)
                continue
            }
                
            Object event = link.nextEvent()
            if (event instanceof ChatMessageEvent)
                handleChatMessage(event)
            else if (event instanceof Persona)
                handleLeave(event)
            else
                throw new IllegalArgumentException("unknown event type $event")
        }
    }
    
    private void handleChatMessage(ChatMessageEvent e) {
        String text = DataHelper.formatTime(e.timestamp)+" <"+e.sender.getHumanReadableName()+ "> ["+
            e.room+"] "+e.payload
        guiThread.invokeLater({textBox.addLine(text)})
    }
    
    private void handleLeave(Persona p) {
        guiThread.invokeLater({textBox.addLine(p.getHumanReadableName()+ " disconnected")})
    }
}
