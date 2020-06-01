package com.muwire.gui.wizard

import java.awt.Component

import com.muwire.core.MuWireSettings

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class WizardModel {
    Component parent
    WizardDefaults defaults
    boolean embeddedRouterAvailable
    MuWireSettings muSettings
    Properties i2pProps
    def finished
    
    final List<WizardStep> steps = []
    int currentStep
    
    @Observable boolean finishButtonEnabled
    @Observable boolean previousButtonEnabled
    @Observable boolean nextButtonEnabled
    
    void mvcGroupInit(Map<String,String> args) {
        
        steps << new NicknameStep()
        steps << new DirectoriesStep(defaults)
        if (embeddedRouterAvailable)
            steps << new EmbeddedRouterStep(defaults)
        else
            steps << new ExternalRouterStep(defaults)
        steps << new TunnelStep(defaults)
        steps << new LastStep(embeddedRouterAvailable)
        
        currentStep = 0
        previousButtonEnabled = false
        nextButtonEnabled = steps.size() > (currentStep + 1)
        finishButtonEnabled = steps.size() == currentStep + 1
    }
}