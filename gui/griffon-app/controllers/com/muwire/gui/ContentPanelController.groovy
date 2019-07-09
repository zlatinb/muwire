package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class ContentPanelController {
    @MVCMember @Nonnull
    ContentPanelModel model

    @ControllerAction
    void addRule() {
    }
    
    @ControllerAction
    void deleteRule() {
        
    }
    
    @ControllerAction
    void keyword() {
        model.regex = false
    }
    
    @ControllerAction
    void regex() {
        model.regex = true
    }
}