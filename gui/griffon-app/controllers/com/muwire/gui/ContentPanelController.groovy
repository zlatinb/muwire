package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.EventBus
import com.muwire.core.content.ContentControlEvent

@ArtifactProviderFor(GriffonController)
class ContentPanelController {
    @MVCMember @Nonnull
    ContentPanelModel model
    @MVCMember @Nonnull
    ContentPanelView view
    
    EventBus eventBus

    @ControllerAction
    void addRule() {
        def term = view.ruleTextField.text
        eventBus.publish(new ContentControlEvent(term : term, regex : model.regex, add:true))
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