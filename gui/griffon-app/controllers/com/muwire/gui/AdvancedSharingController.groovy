package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.Core

@ArtifactProviderFor(GriffonController)
class AdvancedSharingController {
    @MVCMember @Nonnull
    AdvancedSharingModel model
    @MVCMember @Nonnull
    AdvancedSharingView view
}