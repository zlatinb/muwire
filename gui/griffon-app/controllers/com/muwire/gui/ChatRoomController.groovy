package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import groovy.util.logging.Log
import net.i2p.crypto.DSAEngine
import net.i2p.data.DataHelper
import net.i2p.data.Signature

import java.nio.charset.StandardCharsets
import java.util.logging.Level

import javax.annotation.Nonnull

import com.muwire.core.Persona
import com.muwire.core.chat.ChatCommand
import com.muwire.core.chat.ChatAction
import com.muwire.core.chat.ChatConnection
import com.muwire.core.chat.ChatMessageEvent
import com.muwire.core.chat.ChatServer

@Log
@ArtifactProviderFor(GriffonController)
class ChatRoomController {
    @MVCMember @Nonnull
    ChatRoomModel model
    @MVCMember @Nonnull
    ChatRoomView view

    @ControllerAction
    void say() {
        String words = view.sayField.text
        view.sayField.setText(null)
        
        ChatCommand command
        try {
            command = new ChatCommand(words)
        } catch (Exception nope) {
            command = new ChatCommand("/SAY $words")
        }
        
        long now = System.currentTimeMillis()
        UUID uuid = UUID.randomUUID()
        String room = model.console ? ChatServer.CONSOLE : model.room

        byte [] sig = ChatConnection.sign(uuid, now, room, command.source, model.core.me, mvcGroup.parentGroup.model.host, model.core.spk)

        def event = new ChatMessageEvent(uuid : uuid,
        payload : command.source,
        sender : model.core.me,
        host : mvcGroup.parentGroup.model.host,
        room : room,
        chatTime : now,
        sig : sig)

        model.core.eventBus.publish(event)
        if (command.payload.length() > 0) {
            String toShow = DataHelper.formatTime(now) + " <" + model.core.me.getHumanReadableName() + "> "+command.payload

            view.roomTextArea.append(toShow)
            view.roomTextArea.append('\n')
        }
    }
    
    void handleChatMessage(ChatMessageEvent e) {
        ChatCommand command
        try {
            command = new ChatCommand(e.payload)
        } catch (Exception bad) {
            log.log(Level.WARNING,"bad chat command",bad)
            return
        }
        
        switch(command.action) {
            case ChatAction.SAY : processSay(e, command.payload);break
            case ChatAction.JOIN : processJoin(e.timestamp, e.sender); break
            case ChatAction.LEAVE : processLeave(e.timestamp, e.sender); break
        }
    }
    
    private void processSay(ChatMessageEvent e, String text) {
        log.info "processing say $text"
        String toDisplay = DataHelper.formatTime(e.timestamp) + " <"+e.sender.getHumanReadableName()+"> " + text + "\n"
        runInsideUIAsync {
            view.roomTextArea.append(toDisplay)
        }
    }
    
    private void processJoin(long timestamp, Persona p) {
        String toDisplay = DataHelper.formatTime(timestamp) + " " + p.getHumanReadableName() + " joined the room\n"
        runInsideUIAsync {
            view.roomTextArea.append(toDisplay)
        }
    }
    
    private void processLeave(long timestamp, Persona p) {
        String toDisplay = DataHelper.formatTime(timestamp) + " " + p.getHumanReadableName() + " left the room\n"
        runInsideUIAsync {
            view.roomTextArea.append(toDisplay)
        }
    }
    
    void handleLeave(Persona p) {
        String toDisplay = DataHelper.formatTime(System.currentTimeMillis()) + " " + p.getHumanReadableName() + " disconnected\n"
        runInsideUIAsync {
            view.roomTextArea.append(toDisplay)
        }
    }
}