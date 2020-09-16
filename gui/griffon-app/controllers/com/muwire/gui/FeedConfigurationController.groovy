package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.filefeeds.UIFeedConfigurationEvent

@ArtifactProviderFor(GriffonController)
class FeedConfigurationController {
    @MVCMember @Nonnull
    FeedConfigurationModel model
    @MVCMember @Nonnull
    FeedConfigurationView view

    @ControllerAction
    void save() {
        
        model.feed.setAutoDownload(view.autoDownloadCheckbox.model.isSelected())
        model.feed.setSequential(view.sequentialCheckbox.model.isSelected())
        model.feed.setItemsToKeep(Integer.parseInt(view.itemsToKeepField.text))
        model.feed.setUpdateInterval(Long.parseLong(view.updateIntervalField.text) * 60000)
        
        model.core.eventBus.publish(new UIFeedConfigurationEvent(feed : model.feed))
        
        cancel()    
    }
    
    @ControllerAction
    void cancel() {
        view.dialog.setVisible(false)
        mvcGroup.destroy()
    }
}