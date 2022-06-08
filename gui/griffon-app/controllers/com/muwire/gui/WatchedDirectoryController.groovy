package com.muwire.gui

import com.muwire.core.Persona
import com.muwire.core.profile.MWProfileHeader
import com.muwire.gui.profile.PersonaOrProfile
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.files.directories.WatchedDirectoryConfigurationEvent

import java.util.stream.Collectors

@ArtifactProviderFor(GriffonController)
class WatchedDirectoryController {
    @MVCMember @Nonnull
    WatchedDirectoryModel model
    @MVCMember @Nonnull
    WatchedDirectoryView view

    @ControllerAction
    void save() {
        Set<PersonaOrProfile> selectedPOPs = view.contactChooser.getSelectedPOPs()
        Set<Persona> customVisibility = selectedPOPs.collect {it.getPersona()}
        Set<MWProfileHeader> customVisibilityHeaders = selectedPOPs.stream().
                filter({it.getHeader() != null}).
                map({it.getHeader()}).
                collect(Collectors.toSet())
        
        def event = new WatchedDirectoryConfigurationEvent(
            directory : model.directory.directory,
            autoWatch : view.autoWatchCheckbox.model.isSelected(),
            syncInterval : Integer.parseInt(view.syncIntervalField.text),
            subfolders: view.applySubCheckbox.model.isSelected(),
            visibility: model.visibility,
            customVisibility: customVisibility,
            customVisibilityHeaders: customVisibilityHeaders)
        model.core.eventBus.publish(event)
        cancel()
    }
    
    @ControllerAction
    void cancel() {
        view.window.setVisible(false)
        mvcGroup.destroy()
    }
}