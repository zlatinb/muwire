package com.muwire.gui

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named

import com.muwire.core.Core

import griffon.core.artifact.GriffonModel
import griffon.core.GriffonApplication
import griffon.inject.Contextual
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class EventListModel {
    @Inject @Nonnull GriffonApplication application
    @Observable boolean coreInitialized = false

    void mvcGroupInit(Map<String, Object> args) {
        application.addPropertyChangeListener("core", {e ->
            coreInitialized = (e.getNewValue() != null)
        })
    }
}