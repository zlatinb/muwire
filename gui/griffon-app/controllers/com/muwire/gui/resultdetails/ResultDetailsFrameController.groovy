package com.muwire.gui.resultdetails

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class ResultDetailsFrameController {
    @MVCMember @Nonnull
    ResultDetailsFrameView view
    
    @ControllerAction
    void close() {
        view.window.setVisible(false)
        mvcGroup.destroy()
    }
}
