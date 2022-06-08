package com.muwire.gui.contacts

import com.muwire.gui.UISettings
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.PersonaOrProfileCellRenderer

import javax.swing.JComboBox
import javax.swing.JTextField

class ContactChooser extends JComboBox{
    private final ContactChooserEditor chooserEditor
    ContactChooser(UISettings settings, ContactChooserModel model) {
        setModel(model)
        chooserEditor = new ContactChooserEditor(model, this, settings)
        setEditor(chooserEditor)
        setRenderer(new PersonaOrProfileListCellRenderer(settings))
        setEditable(true)
    }
    
    void loadPOPs(Set<PersonaOrProfile> pops) {
        pops.each {chooserEditor.textPane.insertPOP(it)}
    }
    
    Set<PersonaOrProfile> getSelectedPOPs() {
        chooserEditor.textPane.getSelectedPOPs()
    }
}
