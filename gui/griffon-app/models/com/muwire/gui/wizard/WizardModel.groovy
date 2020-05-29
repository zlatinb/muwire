package com.muwire.gui.wizard

import java.awt.Component

import com.muwire.core.MuWireSettings

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class WizardModel {
    Component parent
    boolean embeddedRouterAvailable
    MuWireSettings muSettings
    Properties i2pProps
    def finished
    
    final List<WizardStep> steps = [new NicknameStep(),
                                    new DirectoriesStep(),
                                    new LastStep(embeddedRouterAvailable)]
    int currentStep
    
    @Observable boolean finishButtonEnabled
    @Observable boolean previousButtonEnabled
    @Observable boolean nextButtonEnabled
    
    void mvcGroupInit(Map<String,String> args) {
        currentStep = 0
        previousButtonEnabled = false
        nextButtonEnabled = steps.size() > (currentStep + 1)
        finishButtonEnabled = steps.size() == currentStep + 1
    }
}