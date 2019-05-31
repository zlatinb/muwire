package com.muwire.gui

import javax.annotation.Nonnull
import javax.inject.Inject

import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class MainFrameModel {
    @Inject @Nonnull GriffonApplication application
    @Observable boolean coreInitialized = false
    
    @Observable def results
    @Observable def downloads
    
    void mvcGroupInit(Map<String, Object> args) {
        application.addPropertyChangeListener("core", {e ->
            coreInitialized = (e.getNewValue() != null)
        })
    }
}