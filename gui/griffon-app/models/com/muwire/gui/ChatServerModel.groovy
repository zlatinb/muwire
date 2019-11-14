package com.muwire.gui

import java.util.logging.Level

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.chat.ChatCommand
import com.muwire.core.chat.ChatAction
import com.muwire.core.chat.ChatConnectionAttemptStatus
import com.muwire.core.chat.ChatConnectionEvent
import com.muwire.core.chat.ChatLink
import com.muwire.core.chat.ChatMessageEvent
import com.muwire.core.chat.UIConnectChatEvent

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import groovy.util.logging.Log
import griffon.metadata.ArtifactProviderFor

@Log
@ArtifactProviderFor(GriffonModel)
class ChatServerModel {
    Persona host
    Core core
    
    @Observable boolean disconnectActionEnabled
    @Observable ChatConnectionAttemptStatus status

    volatile ChatLink link    
    volatile Thread poller
    volatile boolean running
    
    void mvcGroupInit(Map<String, String> params) {
        disconnectActionEnabled = host != core.me // can't disconnect from myself
        core.eventBus.register(ChatConnectionEvent.class, this)

        connect()        
    }
    
    void connect() {
        core.eventBus.publish(new UIConnectChatEvent(host : host))
    }
    
    void mvcGroupDestroy() {
        stopPoller()
        core.eventBus.unregister(ChatConnectionEvent.class, this)
    }
    
    private void startPoller() {
        if (running)
            return
        running = true
        poller = new Thread({eventLoop()} as Runnable)
        poller.setDaemon(true)
        poller.start()
    }
    
    private void stopPoller() {
        running = false
        poller?.interrupt()
        link = null
    }
    
    void onChatConnectionEvent(ChatConnectionEvent e) {
        if (e.persona != host)
            return
            
        runInsideUIAsync {
            status = e.status
        }

        if (e.status == ChatConnectionAttemptStatus.SUCCESSFUL) {
            ChatLink link = e.connection
            if (link == null)
                return
            this.link = e.connection
            
            startPoller()

            mvcGroup.childrenGroups.each {k,v ->
                v.controller.rejoinRoom()
            }
        } else {
            stopPoller()
        }
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
                throw new IllegalArgumentException("event type $event")
        }
    }
    
    private void handleChatMessage(ChatMessageEvent e) {
        ChatCommand chatCommand
        try {
            chatCommand = new ChatCommand(e.payload)
        } catch (Exception badCommand) {
            log.log(Level.WARNING,"bad chat command",badCommand)
            return
        }
        String room = e.room
        if (chatCommand.action == ChatAction.JOIN) {
            room = chatCommand.payload
        }
        if (chatCommand.action == ChatAction.SAY &&
            room == core.me.toBase64()) {
            String groupId = host.getHumanReadableName()+"-"+e.sender.getHumanReadableName() + "-private-chat"
            if (!mvcGroup.childrenGroups.containsKey(groupId)) {
                def params = [:]
                params['core'] = core
                params['tabName'] = host.getHumanReadableName() + "-chat-rooms"
                params['room'] = e.sender.toBase64()
                params['privateChat'] = true
                params['host'] = host
                params['roomTabName'] = e.sender.getHumanReadableName() 

                mvcGroup.createMVCGroup("chat-room",groupId, params)
            }
            room = groupId
        } else
            room = host.getHumanReadableName()+"-"+room
        mvcGroup.childrenGroups[room]?.controller?.handleChatMessage(e)
    }
    
    private void handleLeave(Persona p) {
        mvcGroup.childrenGroups.each { k, v ->
            v.controller.handleLeave(p)
        }
    }
}