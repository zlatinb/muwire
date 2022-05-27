package com.muwire.gui

import com.muwire.core.Persona

import java.text.Collator

class PersonaComparator implements Comparator<Persona>{
    @Override
    int compare(Persona a, Persona b) {
        return Collator.getInstance().compare(a.getHumanReadableName(), b.getHumanReadableName())
    }
}
