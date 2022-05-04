package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.files.directories.WatchedDirectoryConfigurationEvent

@ArtifactProviderFor(GriffonController)
class WatchedDirectoryController {
    @MVCMember @Nonnull
    WatchedDirectoryModel model
    @MVCMember @Nonnull
    WatchedDirectoryView view

    @ControllerAction
    void save() {
        def event = new WatchedDirectoryConfigurationEvent(
            directory : model.directory.directory,
            autoWatch : view.autoWatchCheckbox.model.isSelected(),
            syncInterval : Integer.parseInt(view.syncIntervalField.text),
            subfolders: view.applySubCheckbox.model.isSelected(),
            visibility: model.visibility)
        model.core.eventBus.publish(event)
        cancel()
    }
    
    @ControllerAction
    void cancel() {
        view.window.setVisible(false)
        mvcGroup.destroy()
    }
}