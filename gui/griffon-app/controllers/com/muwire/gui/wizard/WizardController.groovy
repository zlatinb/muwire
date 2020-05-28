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
        recalcButtons()
        view.updateLayout()
    }
    
    @ControllerAction
    void next() {
        def errors = model.steps[model.currentStep].validate()
        if (errors) {
            String errorMessage = String.join("\n", errors)
            JOptionPane.showMessageDialog(model.parent, errorMessage, "Invalid Input", JOptionPane.ERROR_MESSAGE)
        } else {
            model.currentStep++
            recalcButtons()
            view.updateLayout()
        }
    }
    
    @ControllerAction
    void finish() {
        
    }
    
    private void recalcButtons() {
        model.previousButtonEnabled = model.currentStep > 0
        model.nextButtonEnabled = model.steps.size() > (model.currentStep + 1)
        model.finishButtonEnabled = model.steps.size() == (model.currentStep + 1)
    }
}