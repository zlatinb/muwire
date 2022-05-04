package com.muwire.gui

import com.muwire.core.Persona
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonModel)
class ContactSelectorModel {
    Set<Persona> contacts
}
