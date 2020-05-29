package com.muwire.gui.wizard

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull
import javax.swing.JOptionPane

@ArtifactProviderFor(GriffonController)
class WizardController {
    @MVCMember @Nonnull
    WizardModel model
    @MVCMember @Nonnull
    WizardView view

    @ControllerAction
    void previous() {
        model.currentStep--
        view.updateLayout()
    }
    
    @ControllerAction
    void next() {
        def errors = model.steps[model.currentStep].validate()
        if (errors != null && !errors.isEmpty()) {
            String errorMessage = String.join("\n", errors)
            JOptionPane.showMessageDialog(model.parent, errorMessage, "Invalid Input", JOptionPane.ERROR_MESSAGE)
        } else {
            model.currentStep++
            view.updateLayout()
        }
    }
    
    @ControllerAction
    void finish() {
        model.steps.each { 
            it.apply(model.muSettings, model.i2pProps)
        }
        model.finished['applied'] = true
        view.hide()
    }
    
    @ControllerAction
    void cancel() {
        model.finished['applied'] = false
        view.hide()
    }
}