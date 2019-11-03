package com.muwire.gui

import java.awt.event.KeyEvent

import javax.swing.JComboBox

class SearchField extends JComboBox {
    SearchField(SearchFieldModel model) {
        super()
        setEditable(true)
        setModel(model)
        setEditor(new SearchFieldEditor(model, this))
    }
}
