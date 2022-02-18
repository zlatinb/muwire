package com.muwire.gui.tools

import com.muwire.core.content.ContentControlEvent
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class RuleWizardController {
    @MVCMember @Nonnull
    RuleWizardModel model
    @MVCMember @Nonnull
    RuleWizardView view
    
    @ControllerAction
    void save() {
        def event = new ContentControlEvent(add: true,
                term: view.termField.text,
                name: view.nameField.text,
                regex: model.regex,
                action: model.action)
        model.core.eventBus.publish(event)
        cancel()
    }
    
    @ControllerAction
    void cancel() {
        view.dialog.setVisible(false)
    }
}
