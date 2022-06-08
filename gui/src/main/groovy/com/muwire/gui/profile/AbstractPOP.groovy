package com.muwire.gui.profile

/**
 * Implementations of this class are useful for storing
 * in collections
 */
abstract class AbstractPOP implements PersonaOrProfile {

    int hashCode() {
        getPersona().hashCode()
    }

    boolean equals(Object o) {
        if (!(o instanceof PersonaOrProfile))
            return false
        getPersona() == o.getPersona()
    }
    
}
