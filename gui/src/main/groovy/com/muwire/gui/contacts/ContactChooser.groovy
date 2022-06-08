package com.muwire.gui.contacts

import com.muwire.gui.UISettings
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.PersonaOrProfileCellRenderer

import javax.swing.JComboBox
import javax.swing.JTextField

class ContactChooser extends JComboBox{
    private final ContactChooserEditor chooserEditor
    private final ContactChooserModel chooserModel
    ContactChooser(UISettings settings, ContactChooserModel model) {
        setModel(model)
        this.chooserModel = model
        chooserEditor = new ContactChooserEditor(model, this, settings)
        setEditor(chooserEditor)
        setRenderer(new PersonaOrProfileListCellRenderer(settings))
        setEditable(true)
    }
    
    void loadPOPs(Set<PersonaOrProfile> pops) {
        pops.each {chooserEditor.textPane.insertPOP(it)}
    }
    
    Set<PersonaOrProfile> getSelectedPOPs() {
        Set<PersonaOrProfile> rv = chooserEditor.textPane.getSelectedPOPs()
        def lastPOP = chooserEditor.getItem()
        if (lastPOP == null)
            return rv
        lastPOP = chooserModel.findByName(lastPOP.toString())
        if (lastPOP != null)
            rv << lastPOP
        rv
    }
}
