package com.muwire.clilanterna

import com.muwire.core.Persona

class PersonaWrapper {
    private final Persona persona
    PersonaWrapper(Persona persona) {
        this.persona = persona
    }
    
    @Override
    public String toString() {
        persona.getHumanReadableName()
    }
}
