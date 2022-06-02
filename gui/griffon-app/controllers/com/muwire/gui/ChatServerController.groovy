package com.muwire.gui

import com.muwire.core.chat.ChatConnection
import com.muwire.core.chat.ChatMessageEvent
import com.muwire.core.chat.ChatServer
import com.muwire.core.chat.LocalChatLink
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.chat.UIDisconnectChatEvent

import javax.inject.Inject

@ArtifactProviderFor(GriffonController)
class ChatServerController {
    @Inject
    GriffonApplication application
    @MVCMember @Nonnull
    ChatServerModel model
    @MVCMember @Nonnull
    ChatServerView view

    
    void joinRoom(String room) {
        String groupName = model.host.getHumanReadableName() + "-" + room
        def group = application.mvcGroupManager.findGroup(groupName)
        if (group != null)
            return
        
        UUID uuid = UUID.randomUUID()
        String command = "/JOIN $room"
        long now = System.currentTimeMillis()
        byte [] sig = ChatConnection.sign(uuid, now, ChatServer.CONSOLE, command,
            model.core.me, model.host, model.core.spk)
        def event = new ChatMessageEvent(uuid: uuid,
                payload: command,
                sender: model.core.me,
                host: model.host,
                link: LocalChatLink.INSTANCE,
                room: ChatServer.CONSOLE,
                chatTime: now,
                sig: sig)
        model.core.eventBus.publish(event)
        
        def params = [:]
        params['core'] = model.core
        params['tabName'] = model.host.getHumanReadableName() + "-chat-rooms"
        params['room'] = room
        params['roomTabName'] = HTMLSanitizer.sanitize(room)
        params['console'] = false
        params['host'] = model.host
        params['chatNotificator'] = view.chatNotificator
        mvcGroup.createMVCGroup("chat-room", groupName, params)
    }
    
    @ControllerAction
    void disconnect() {
        switch(model.buttonText) {
            case "DISCONNECT" :
                model.buttonText = "CONNECT"
                mvcGroup.getChildrenGroups().each { k,v ->
                    v.controller.serverDisconnected()
                }
                model.core.eventBus.publish(new UIDisconnectChatEvent(host : model.host))
                break
            case "CONNECT" :
                 model.connect()
                 break
        }
    }
}