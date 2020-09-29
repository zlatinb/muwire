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