package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.Core
import com.muwire.core.files.directories.UISyncDirectoryEvent

@ArtifactProviderFor(GriffonController)
class AdvancedSharingController {
    @MVCMember @Nonnull
    AdvancedSharingModel model
    @MVCMember @Nonnull
    AdvancedSharingView view
    
    @ControllerAction
    void configure() {
        def wd = view.selectedWatchedDirectory()
        if (wd == null)
            return
            
        def params = [:]
        params['core'] = model.core
        params['directory'] = wd
        mvcGroup.createMVCGroup("watched-directory",params)
    }
    
    @ControllerAction
    void sync() {
        def wd = view.selectedWatchedDirectory()
        if (wd == null)
            return
        def event = new UISyncDirectoryEvent(directory : wd.directory)
        model.core.eventBus.publish(event)
    }
    
    @ControllerAction
    void close() {
        view.dialog.setVisible(false)
    }
}