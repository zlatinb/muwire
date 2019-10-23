package com.muwire.gui

import javax.annotation.Nonnull

import griffon.core.artifact.GriffonController
import griffon.core.artifact.GriffonModel
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonModel)
class CloseWarningModel {
    @griffon.transform.Observable boolean closeWarning
}