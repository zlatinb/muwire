package com.muwire.gui

import com.muwire.core.Persona

class PersonaComparator implements Comparator<Persona>{
    @Override
    int compare(Persona a, Persona b) {
        return String.compare(a.getHumanReadableName(), b.getHumanReadableName())
    }
}
