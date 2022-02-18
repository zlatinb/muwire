package com.muwire.gui

import com.muwire.core.SharedFile
import com.muwire.core.filefeeds.UIFileUnpublishedEvent
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class MyFeedController {
    @MVCMember @Nonnull
    MyFeedModel model
    @MVCMember @Nonnull
    MyFeedView view
    
    @ControllerAction
    void unpublish() {
        List<SharedFile> toUnpublish = view.selectedItems()
        if (toUnpublish == null)
            return
        toUnpublish.each {
            it.unpublish()
            UIFileUnpublishedEvent event = new UIFileUnpublishedEvent(sf: it)
            model.core.getEventBus().publish(event)
        }
        model.items.removeAll(toUnpublish)
        model.itemsCount = model.items.size()
        view.refreshItemsTable()
    }
    
    @ControllerAction
    void close() {
        view.dialog.setVisible(false)
    }
}