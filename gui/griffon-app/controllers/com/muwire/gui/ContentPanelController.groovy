package com.muwire.gui

import com.muwire.core.content.MatchAction
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
        def props = [:]
        props.core = core
        mvcGroup.createMVCGroup("rule-wizard",props).destroy()
    }
    
    @ControllerAction
    void deleteRule() {
        int rule = view.getSelectedRule()
        if (rule < 0)
            return
        Matcher matcher = model.rules[rule]
        
        core.eventBus.publish(new ContentControlEvent(matcher: matcher, add: false))
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
    
    @ControllerAction
    void close() {
        view.dialog.setVisible(false)
    }
}