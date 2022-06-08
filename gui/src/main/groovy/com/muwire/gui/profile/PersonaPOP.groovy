package com.muwire.gui.profile

import com.muwire.core.Persona

import javax.swing.Icon

/**
 * Implementation of PersonaAndProfile when only 
 * a Persona is available.
 */
class PersonaPOP extends AbstractPOP {
    final Persona persona
    PersonaPOP(Persona persona) {
        this.persona = persona
    }

    Persona getPersona() {
        persona
    }

    @Override
    Icon getThumbnail() {
        return null
    }
}
