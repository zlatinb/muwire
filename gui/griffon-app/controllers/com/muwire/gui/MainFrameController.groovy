package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class MainFrameController {
    @MVCMember @Nonnull
    MainFrameModel model

    @ControllerAction
    void click() {
        model.clickCount++
    }
}