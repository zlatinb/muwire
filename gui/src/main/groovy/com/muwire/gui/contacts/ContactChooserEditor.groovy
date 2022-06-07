package com.muwire.gui.contacts

import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.basic.BasicComboBoxEditor
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

class ContactChooserEditor extends BasicComboBoxEditor{
    
    private final ContactChooserModel model
    private final ContactChooser field
    
    ContactChooserEditor(ContactChooserModel model, ContactChooser field) {
        super()
        this.model = model
        this.field = field

        editor.addKeyListener(new KeyAdapter() {
            @Override
            void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode()
                if (keyCode == KeyEvent.VK_ENTER)
                    editor.setText("")
                else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
                    return
                } else {
                    SwingUtilities.invokeLater {
                        field.hidePopup()
                        if (model.onKeyStroke(editor.text))
                            field.showPopup()
                    }
                }
            }
        })
    }
    
    public void setItem(Object o) {
        if (o == null || o instanceof ContactChooserPOP)
            super.setItem(o)
        else
            editor.setText("")
    }
}
