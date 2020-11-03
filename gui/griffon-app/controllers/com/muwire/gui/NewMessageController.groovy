package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.Persona
import com.muwire.core.messenger.MWMessage
import com.muwire.core.messenger.UIMessageEvent

@ArtifactProviderFor(GriffonController)
class NewMessageController {
    @MVCMember @Nonnull
    NewMessageModel model
    @MVCMember @Nonnull
    NewMessageView view
    
    @ControllerAction
    void send() {
        Set<Persona> recipients = new HashSet<>()
        recipients.add(model.recipient)
        MWMessage message = new MWMessage(model.core.me, recipients, view.subjectField.text, 
            System.currentTimeMillis(), view.bodyArea.getText(), Collections.emptySet(), model.core.spk)
        model.core.eventBus.publish(new UIMessageEvent(message : message))
        
        application.mvcGroupManager.groups["MainFrame"].model.addToOutbox(message)
        
        cancel()
    }
    
    @ControllerAction
    void cancel() {
        view.window.setVisible(false)
        mvcGroup.destroy()
    }
}