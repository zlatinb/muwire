package com.muwire.gui

import com.muwire.core.SharedFile
import com.muwire.core.filefeeds.UIFilePublishedEvent
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class PublishPreviewController {
    
    @MVCMember @Nonnull
    PublishPreviewModel model
    @MVCMember @Nonnull
    PublishPreviewView view
    
    @ControllerAction
    void publish() {
        final long now = System.currentTimeMillis()
        for (SharedFile sf : model.toPublish) {
            sf.publish(now)
            model.core.eventBus.publish(new UIFilePublishedEvent(sf: sf))
        }
        cancel()
    }
    
    @ControllerAction
    void cancel() {
        view.dialog.setVisible(false)
    }
}
