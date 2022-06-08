package com.muwire.gui.contacts

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.PersonaPOP
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.swing.Icon

@ArtifactProviderFor(GriffonModel)
class ContactSelectorModel {
    Core core
    Set<Persona> contacts
    Set<PersonaOrProfile> contactsPOP
    
    Set<PersonaOrProfile> allContacts = new HashSet<>()
    
    void mvcGroupInit(Map<String,String> args) {
        if (contacts != null) {
            contacts.each {allContacts.add(new PersonaPOP(it))}
        }
        if (contactsPOP != null) {
            allContacts.addAll(contactsPOP)
        }
    }
}
