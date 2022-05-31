package com.muwire.gui.profile

import java.text.Collator

class PersonaOrProfileComparator implements Comparator<PersonaOrProfile>{
    int compare(PersonaOrProfile a, PersonaOrProfile b) {
        Collator.getInstance().compare(a.getPersona().getHumanReadableName(), b.getPersona().getHumanReadableName())
    }
}
