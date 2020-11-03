package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class NewMessageController {
    @MVCMember @Nonnull
    NewMessageModel model
    @MVCMember @Nonnull
    NewMessageView view
    
    @ControllerAction
    void send() {
        
    }
    
    @ControllerAction
    void cancel() {
        view.window.setVisible(false)
        mvcGroup.destroy()
    }
}