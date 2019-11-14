package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.chat.UIDisconnectChatEvent

@ArtifactProviderFor(GriffonController)
class ChatServerController {
    @MVCMember @Nonnull
    ChatServerModel model

    @ControllerAction
    void disconnect() {
        switch(model.buttonText) {
            case "Disconnect" :
                model.buttonText = "Connect"
                model.core.eventBus.publish(new UIDisconnectChatEvent(host : model.host))
                break
            case "Connect" :
                 model.connect()
                 break
        }
    }
}