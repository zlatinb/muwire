package com.muwire.gui.contacts

import com.muwire.gui.UISettings
import com.muwire.gui.profile.PersonaOrProfileCellRenderer

import javax.swing.JComboBox
import javax.swing.JTextField

class ContactChooser extends JComboBox{
    ContactChooser(UISettings settings, ContactChooserModel model) {
        super()
        setEditable(true)
        setModel(model)
        setEditor(new ContactChooserEditor(model, this))
        setRenderer(new PersonaOrProfileListCellRenderer(settings))
        
    }
}
