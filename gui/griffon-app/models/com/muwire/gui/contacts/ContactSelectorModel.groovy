package com.muwire.gui.contacts

import com.muwire.core.Core
import com.muwire.core.Persona
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonModel)
class ContactSelectorModel {
    Core core
    Set<Persona> contacts
}
