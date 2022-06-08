package com.muwire.gui.contacts

import com.muwire.core.trust.TrustService
import com.muwire.core.trust.TrustService.TrustEntry
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.PersonaOrProfileComparator
import com.muwire.gui.profile.TrustPOP

import javax.swing.ComboBoxModel
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.MutableComboBoxModel

class ContactChooserModel extends DefaultComboBoxModel implements MutableComboBoxModel {
    private final List<PersonaOrProfile> allObjects
    private final Comparator<PersonaOrProfile> popComparator = new PersonaOrProfileComparator()
    PersonaOrProfile selectedPOP
    
    ContactChooserModel(Collection<TrustEntry> entries) {
        allObjects = entries.collect {new ContactChooserPOP(it)}
        addAll(allObjects)
    }
    
    boolean onKeyStroke(String selected) {
        
        if (selected == null || selected.length() == 0) {
            removeAllElements()
            addAll(allObjects)
            return true
        }
        
        removeAllElements()
        setSelectedItem(new ContactChooserPOP(selected))
        
        List<PersonaOrProfile> matches = []
        for(PersonaOrProfile pop : allObjects) {
            if (justName(pop).containsIgnoreCase(selected))
                matches << pop
        }
        if (matches.isEmpty())
            return false
        
        Collections.sort(matches, popComparator)
        addAll(matches)
        true
    }
    
    private static String justName(PersonaOrProfile pop) {
        String name = pop.getPersona().getHumanReadableName()
        name.substring(0, name.indexOf("@")).toLowerCase()
    }
    
    @Override
    public void setSelectedItem(Object anObject) {
        println "CCM.setSelectedItem $anObject"
        if (anObject instanceof String) {
            if (anObject == selectedPOP?.getPersona()?.getHumanReadableName())
                super.setSelectedItem(selectedPOP)
            else
                super.setSelectedItem(new ContactChooserPOP(anObject))
            return
        }
        if (anObject == null)
            selectedPOP = null
        else {
            if (!(anObject instanceof ContactChooserPOP))
                throw new Exception("invalid type $anObject")

            ContactChooserPOP ccp = (ContactChooserPOP) anObject
            if (ccp.getPersona() != null)
                selectedPOP = ccp
        }
        super.setSelectedItem(anObject)
    }

    PersonaOrProfile findByName(String name) {
        allObjects.find {it.getPersona().getHumanReadableName() == name}
    }
}
