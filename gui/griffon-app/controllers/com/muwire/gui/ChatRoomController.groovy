package com.muwire.gui

import static com.muwire.gui.Translator.trans

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
import javax.swing.text.StyledDocument

import com.muwire.core.Persona
import com.muwire.core.chat.ChatCommand
import com.muwire.core.chat.ChatAction
import com.muwire.core.chat.ChatConnection
import com.muwire.core.chat.ChatMessageEvent
import com.muwire.core.chat.ChatServer
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

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
            JOptionPane.showMessageDialog(null, trans("NOT_USER_COMMAND",words),trans("INVALID_COMMAND"), JOptionPane.ERROR_MESSAGE)
            return
        }
        long now = System.currentTimeMillis()
        
        if (command.action == ChatAction.SAY && command.payload.length() > 0) {
            String header = DataHelper.formatTime(now) + " <" + model.core.me.getHumanReadableName() + ">"
            StyledDocument sd = view.roomTextArea.getStyledDocument()
            sd.insertString(sd.getEndPosition().getOffset() - 1, header, sd.getStyle("italic"))
            sd.insertString(sd.getEndPosition().getOffset() - 1, " ", sd.getStyle("regular"))
            sd.insertString(sd.getEndPosition().getOffset() - 1, command.payload, sd.getStyle("regular"))
            sd.insertString(sd.getEndPosition().getOffset() - 1, "\n", sd.getStyle("regular"))

            trimLines()
        }
        
        if (command.action == ChatAction.JOIN && model.console) {
            String newRoom = command.payload
            String groupId = model.host.getHumanReadableName()+"-"+newRoom
            if (!mvcGroup.parentGroup.childrenGroups.containsKey(groupId)) {
                def params = [:]
                params['core'] = model.core
                params['tabName'] = model.host.getHumanReadableName() + "-chat-rooms"
                params['room'] = newRoom
                params['console'] = false
                params['host'] = model.host
                params['roomTabName'] = newRoom
                params['chatNotificator'] = view.chatNotificator

                mvcGroup.parentGroup.createMVCGroup("chat-room", groupId, params)
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
        String groupId = model.host.getHumanReadableName() + "-" + p.getHumanReadableName() +"-private-chat"
        if (p != model.core.me && !mvcGroup.parentGroup.childrenGroups.containsKey(groupId)) {
            def params = [:]
            params['core'] = model.core
            params['tabName'] = model.tabName
            params['room'] = p.toBase64()
            params['privateChat'] = true
            params['host'] = model.host
            params['roomTabName'] = p.getHumanReadableName()
            params['chatNotificator'] = view.chatNotificator
            
            mvcGroup.parentGroup.createMVCGroup("chat-room", groupId, params)
        }
    }
    
    void markTrusted() {
        Persona p = view.getSelectedPersona()
        if (p == null)
            return
        String reason = JOptionPane.showInputDialog(trans("ENTER_REASON_OPTIONAL"))
        model.core.eventBus.publish(new TrustEvent(persona : p, level : TrustLevel.TRUSTED, reason : reason))
        view.refreshMembersTable()    
    }
    
    void markDistrusted() {
        Persona p = view.getSelectedPersona()
        if (p == null)
            return
        String reason = JOptionPane.showInputDialog(trans("ENTER_REASON_OPTIONAL"))
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
    
    void browse() {
        Persona p = view.getSelectedPersona()
        if (p == null)
            return
        String groupId = p.getHumanReadableName() + "-browse"
        def params = [:]
        params['host'] = p
        params['core'] = model.core
        mvcGroup.createMVCGroup("browse",groupId,params)
        application.mvcGroupManager.findGroup("MainFrame").view.showSearchWindow.call()
    }
    
    void leaveRoom() {
        if (leftRoom || model.privateChat)
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
        runInsideUIAsync {
            view.appendSay(text, e.sender, e.timestamp)
            trimLines()
            if (!model.console)
                view.chatNotificator.onMessage(mvcGroup.mvcId)
        }
    }
    
    private void processJoin(long timestamp, Persona p) {
        String toDisplay = DataHelper.formatTime(timestamp) + " " + trans("JOINED_ROOM", p.getHumanReadableName()) + "\n"
        runInsideUIAsync {
            model.members.add(p)
            view.appendGray(toDisplay)
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
        String toDisplay = DataHelper.formatTime(timestamp) + " " + trans("LEFT_ROOM",p.getHumanReadableName()) + "\n"
        runInsideUIAsync {
            model.members.remove(p)
            view.appendGray(toDisplay)
            trimLines()
            view.membersTable?.model?.fireTableDataChanged()
        }
    }
    
    void handleLeave(Persona p) {
        String toDisplay = DataHelper.formatTime(System.currentTimeMillis()) + " " + trans("USER_DISCONNECTED",p.getHumanReadableName()) + "\n"
        runInsideUIAsync {
            if (model.members.remove(p)) {
                view.appendGray(toDisplay)
                trimLines()
                view.membersTable?.model?.fireTableDataChanged()
            }
        }
    }
    
    private void trimLines() {
        if (model.settings.maxChatLines < 0)
            return
        while(view.getLineCount() > model.settings.maxChatLines) 
            view.removeFirstLine()
    }
    
    void rejoinRoom() {
        if (model.console || model.privateChat)
            return
        
        model.members.clear()
        model.members.add(model.core.me)
        
        UUID uuid = UUID.randomUUID()
        long now = System.currentTimeMillis()
        String join = "/JOIN $model.room"
        byte [] sig = ChatConnection.sign(uuid, now, ChatServer.CONSOLE, join, model.core.me, model.host, model.core.spk)
        def event = new ChatMessageEvent(
            uuid : uuid,
            payload : join,
            sender : model.core.me,
            host : model.host,
            room : ChatServer.CONSOLE,
            chatTime : now,
            sig : sig
        )
        model.core.eventBus.publish(event)
    }
    
    void serverDisconnected() {
        runInsideUIAsync {
            model.members.clear()
            view.membersTable?.model?.fireTableDataChanged()
        }
    }
}