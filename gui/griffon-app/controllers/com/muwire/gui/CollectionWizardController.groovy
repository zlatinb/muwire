package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class CollectionWizardController {
    @MVCMember @Nonnull
    CollectionWizardModel model
    @MVCMember @Nonnull
    CollectionWizardView view

    @ControllerAction
    void cancel() {
        view.dialog.setVisible(false)
        mvcGroup.destroy()
    }
    
    @ControllerAction
    void review() {
        cancel()
    }
}