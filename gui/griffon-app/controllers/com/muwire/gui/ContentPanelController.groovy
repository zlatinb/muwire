package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.Core
import com.muwire.core.EventBus
import com.muwire.core.content.ContentControlEvent
import com.muwire.core.content.Matcher
import com.muwire.core.content.RegexMatcher

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
        int rule = view.getSelectedRule()
        if (rule < 0)
            return
        Matcher matcher = model.rules[rule]
        String term = matcher.getTerm()
        if (matcher instanceof RegexMatcher) 
            core.muOptions.watchedRegexes.remove(term)
        else
            core.muOptions.watchedKeywords.remove(term)
        saveMuWireSettings()
        
        core.eventBus.publish(new ContentControlEvent(term : term, regex : (matcher instanceof RegexMatcher), add: false))
    }
    
    @ControllerAction
    void keyword() {
        model.regex = false
    }
    
    @ControllerAction
    void regex() {
        model.regex = true
    }
    
    @ControllerAction
    void refresh() {
        model.refresh()
    }
    
    @ControllerAction
    void trust() {
        
    }
    
    @ControllerAction
    void distrust() {
        
    }
    
    void saveMuWireSettings() {
        File f = new File(core.home, "MuWire.properties")
        f.withOutputStream {
            core.muOptions.write(it)
        }
    }
}