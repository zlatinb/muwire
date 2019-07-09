package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.Core
import com.muwire.core.EventBus
import com.muwire.core.content.ContentControlEvent

@ArtifactProviderFor(GriffonController)
class ContentPanelController {
    @MVCMember @Nonnull
    ContentPanelModel model
    @MVCMember @Nonnull
    ContentPanelView view
    
    Core core

    @ControllerAction
    void addRule() {
        def term = view.ruleTextField.text
        
        if (model.regex)
            core.muOptions.watchedRegexes.add(term)
        else
            core.muOptions.watchedKeywords.add(term)
        saveMuWireSettings()
           
        core.eventBus.publish(new ContentControlEvent(term : term, regex : model.regex, add:true))
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
    
    void saveMuWireSettings() {
        File f = new File(core.home, "MuWire.properties")
        f.withOutputStream {
            core.muOptions.write(it)
        }
    }
}