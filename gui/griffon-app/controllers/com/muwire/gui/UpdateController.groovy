package com.muwire.gui

import com.muwire.core.RestartEvent
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class UpdateController {
    @MVCMember @Nonnull
    UpdateView view
    @MVCMember @Nonnull
    UpdateModel model

    @ControllerAction
    void close() {
        view.dialog.setVisible(false)
    }
    
    @ControllerAction
    void restart() {
        model.core.eventBus.publish(new RestartEvent())
    }
    
    @ControllerAction
    void search() {
        mvcGroup.parentGroup.controller.search(model.available.infoHash, "MuWire update")
        close()
    }
}