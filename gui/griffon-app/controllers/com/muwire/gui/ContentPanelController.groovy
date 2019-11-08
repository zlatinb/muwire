package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull
import javax.swing.JOptionPane

import com.muwire.core.Core
import com.muwire.core.EventBus
import com.muwire.core.content.ContentControlEvent
import com.muwire.core.content.Match
import com.muwire.core.content.Matcher
import com.muwire.core.content.RegexMatcher
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

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
    void clearHits() {
        int selectedRule = view.getSelectedRule()
        if (selectedRule < 0)
            return
        Matcher matcher = model.rules[selectedRule]
        matcher.matches.clear()
        model.refresh()
    }
    
    @ControllerAction
    void trust() {
        int selectedHit = view.getSelectedHit()
        if (selectedHit < 0)
            return
        String reason = JOptionPane.showInputDialog("Enter reason (optional)")
        Match m = model.hits[selectedHit]
        core.eventBus.publish(new TrustEvent(persona : m.persona, level : TrustLevel.TRUSTED, reason : reason))
    }
    
    @ControllerAction
    void distrust() {
        int selectedHit = view.getSelectedHit()
        if (selectedHit < 0)
            return
        String reason = JOptionPane.showInputDialog("Enter reason (optional)")
        Match m = model.hits[selectedHit]
        core.eventBus.publish(new TrustEvent(persona : m.persona, level : TrustLevel.DISTRUSTED, reason : reason))
    }
    
    void saveMuWireSettings() {
        core.saveMuSettings()
    }
}