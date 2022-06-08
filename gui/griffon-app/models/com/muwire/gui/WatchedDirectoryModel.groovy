package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.files.directories.Visibility
import com.muwire.core.files.directories.WatchedDirectory
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.PersonaPOP
import com.muwire.gui.profile.ProfileHeaderPOP
import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class WatchedDirectoryModel {
    Core core
    WatchedDirectory directory
    
    @Observable boolean autoWatch
    @Observable int syncInterval
    @Observable Visibility visibility
    Set<PersonaOrProfile> allowedContacts = new HashSet<>()
    
    void mvcGroupInit(Map<String,String> args) {
        autoWatch = directory.autoWatch
        syncInterval = directory.syncInterval
        visibility = directory.visibility
        if (directory.customVisibilityHeaders != null)
            allowedContacts.addAll(directory.customVisibilityHeaders.collect {new ProfileHeaderPOP(it)})
        if (directory.customVisibility != null)
            allowedContacts.addAll(directory.customVisibility.collect {new PersonaPOP(it)})
    }
}