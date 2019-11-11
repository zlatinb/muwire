package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.crypto.DSAEngine
import net.i2p.data.DataHelper
import net.i2p.data.Signature

import java.nio.charset.StandardCharsets

import javax.annotation.Nonnull

import com.muwire.core.chat.ChatConnection
import com.muwire.core.chat.ChatMessageEvent
import com.muwire.core.chat.ChatServer

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
        
        long now = System.currentTimeMillis()
        UUID uuid = UUID.randomUUID()
        String room = model.console ? ChatServer.CONSOLE : model.room

        byte [] sig = ChatConnection.sign(uuid, now, room, words, model.core.me, mvcGroup.parentGroup.model.host, model.core.spk)
        
        def event = new ChatMessageEvent(uuid : uuid,
            payload : words,
            sender : model.core.me,
            host : mvcGroup.parentGroup.model.host,
            room : room,
            chatTime : now,
            sig : sig)
        
        model.core.eventBus.publish(event)        
        
        String toShow = DataHelper.formatTime(now) + " <" + model.core.me.getHumanReadableName() + "> "+words
        
        view.roomTextArea.append(toShow)
        view.roomTextArea.append('\n')
    }
}