package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import groovy.util.logging.Log
import net.i2p.crypto.DSAEngine
import net.i2p.data.Base64
import net.i2p.data.DataHelper
import net.i2p.data.Signature

import java.nio.charset.StandardCharsets
import java.util.logging.Level

import javax.annotation.Nonnull
import javax.swing.JOptionPane

import com.muwire.core.Persona
import com.muwire.core.chat.ChatCommand
import com.muwire.core.chat.ChatAction
import com.muwire.core.chat.ChatConnection
import com.muwire.core.chat.ChatMessageEvent
import com.muwire.core.chat.ChatServer
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

@Log
@ArtifactProviderFor(GriffonController)
class ChatRoomController {
    @MVCMember @Nonnull
    ChatRoomModel model
    @MVCMember @Nonnull
    ChatRoomView view

    boolean leftRoom
    
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
        
        if (!command.action.user) {
            JOptionPane.showMessageDialog(null, "$words is not a user command","Invalid Command", JOptionPane.ERROR_MESSAGE)
            return
        }
        long now = System.currentTimeMillis()
        
        if (command.action == ChatAction.SAY && command.payload.length() > 0) {
            String toShow = DataHelper.formatTime(now) + " <" + model.core.me.getHumanReadableName() + "> "+command.payload

            view.roomTextArea.append(toShow)
            view.roomTextArea.append('\n')
            trimLines()
        }
        
        if (command.action == ChatAction.JOIN) {
            String newRoom = command.payload
            if (!mvcGroup.parentGroup.childrenGroups.containsKey(newRoom)) {
                def params = [:]
                params['core'] = model.core
                params['tabName'] = model.host.getHumanReadableName() + "-chat-rooms"
                params['room'] = newRoom
                params['console'] = false
                params['host'] = model.host
                params['roomTabName'] = newRoom

                mvcGroup.parentGroup.createMVCGroup("chat-room", newRoom, params)
            }
        }
        if (command.action == ChatAction.LEAVE && !model.console) {
            leftRoom = true
            view.closeTab.call()
        }
        
        String room = model.console ? ChatServer.CONSOLE : model.room
        
        UUID uuid = UUID.randomUUID()
        byte [] sig = ChatConnection.sign(uuid, now, room, command.source, model.core.me, model.host, model.core.spk)

        def event = new ChatMessageEvent(uuid : uuid,
        payload : command.source,
        sender : model.core.me,
        host : model.host,
        room : room,
        chatTime : now,
        sig : sig)

        model.core.eventBus.publish(event)
    }
    
    @ControllerAction
    void privateMessage() { 
        Persona p = view.getSelectedPersona()
        if (p == null)
            return
        if (p != model.core.me && !mvcGroup.parentGroup.childrenGroups.containsKey(p.getHumanReadableName()+"-private-chat")) {
            def params = [:]
            params['core'] = model.core
            params['tabName'] = model.tabName
            params['room'] = p.toBase64()
            params['privateChat'] = true
            params['host'] = model.host
            params['roomTabName'] = p.getHumanReadableName()
            
            mvcGroup.parentGroup.createMVCGroup("chat-room", p.getHumanReadableName()+"-private-chat", params)
        }
    }
    
    void markTrusted() {
        Persona p = view.getSelectedPersona()
        if (p == null)
            return
        String reason = JOptionPane.showInputDialog("Enter reason (optional)")
        model.core.eventBus.publish(new TrustEvent(persona : p, level : TrustLevel.TRUSTED, reason : reason))
        view.refreshMembersTable()    
    }
    
    void markDistrusted() {
        Persona p = view.getSelectedPersona()
        if (p == null)
            return
        String reason = JOptionPane.showInputDialog("Enter reason (optional)")
        model.core.eventBus.publish(new TrustEvent(persona : p, level : TrustLevel.DISTRUSTED, reason : reason))
        view.refreshMembersTable()
    }
    
    void markNeutral() {
        Persona p = view.getSelectedPersona()
        if (p == null)
            return
        model.core.eventBus.publish(new TrustEvent(persona : p, level : TrustLevel.NEUTRAL))
        view.refreshMembersTable()
    }
    
    void leaveRoom() {
        if (leftRoom)
            return
        leftRoom = true
        long now = System.currentTimeMillis()
        UUID uuid = UUID.randomUUID()
        byte [] sig = ChatConnection.sign(uuid, now, model.room, "/LEAVE", model.core.me, model.host, model.core.spk)
        def event = new ChatMessageEvent(uuid : uuid,
            payload : "/LEAVE",
            sender : model.core.me,
            host : model.host,
            room : model.room,
            chatTime : now,
            sig : sig)
        model.core.eventBus.publish(event)
    }
    
    void handleChatMessage(ChatMessageEvent e) {
        ChatCommand command
        try {
            command = new ChatCommand(e.payload)
        } catch (Exception bad) {
            log.log(Level.WARNING,"bad chat command",bad)
            return
        }
        log.info("$model.room processing $command.action")
        switch(command.action) {
            case ChatAction.SAY : processSay(e, command.payload);break
            case ChatAction.JOIN : processJoin(e.timestamp, e.sender); break
            case ChatAction.JOINED : processJoined(command.payload); break
            case ChatAction.LEAVE : processLeave(e.timestamp, e.sender); break
        }
    }
    
    private void processSay(ChatMessageEvent e, String text) {
        String toDisplay = DataHelper.formatTime(e.timestamp) + " <"+e.sender.getHumanReadableName()+"> " + text + "\n"
        runInsideUIAsync {
            view.roomTextArea.append(toDisplay)
            trimLines()
        }
    }
    
    private void processJoin(long timestamp, Persona p) {
        String toDisplay = DataHelper.formatTime(timestamp) + " " + p.getHumanReadableName() + " joined the room\n"
        runInsideUIAsync {
            model.members.add(p)
            view.roomTextArea.append(toDisplay)
            trimLines()
            view.membersTable?.model?.fireTableDataChanged()
        }
    }
    
    private void processJoined(String list) {
        runInsideUIAsync {
            list.split(",").each { 
                Persona p = new Persona(new ByteArrayInputStream(Base64.decode(it)))
                model.members.add(p)
            }
            view.membersTable?.model?.fireTableDataChanged()
        }
    }
    
    private void processLeave(long timestamp, Persona p) {
        String toDisplay = DataHelper.formatTime(timestamp) + " " + p.getHumanReadableName() + " left the room\n"
        runInsideUIAsync {
            model.members.remove(p)
            view.roomTextArea.append(toDisplay)
            trimLines()
            view.membersTable?.model?.fireTableDataChanged()
        }
    }
    
    void handleLeave(Persona p) {
        String toDisplay = DataHelper.formatTime(System.currentTimeMillis()) + " " + p.getHumanReadableName() + " disconnected\n"
        runInsideUIAsync {
            if (model.members.remove(p)) {
                view.roomTextArea.append(toDisplay)
                trimLines()
                view.membersTable?.model?.fireTableDataChanged()
            }
        }
    }
    
    private void trimLines() {
        if (model.settings.maxChatLines < 0)
            return
        while(view.roomTextArea.getLineCount() > model.settings.maxChatLines) {
            int line0Start = view.roomTextArea.getLineStartOffset(0)
            int line0End = view.roomTextArea.getLineEndOffset(0)
            view.roomTextArea.replaceRange(null, line0Start, line0End)
        }
    }
}