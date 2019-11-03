package com.muwire.gui

import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.basic.BasicComboBoxEditor

class SearchFieldEditor extends BasicComboBoxEditor {

    private final SearchFieldModel model
    private final SearchField field

    SearchFieldEditor(SearchFieldModel model, SearchField field) {
        super()
        this.model = model
        this.field = field
        def action = field.getAction()
        field.setAction(null)
        editor.setAction(action)
        editor.getDocument().addDocumentListener(new DocumentListener() {

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        SwingUtilities.invokeLater({
                            field.hidePopup()
                            if (model.onKeyStroke(editor.text))
                                field.showPopup()
                        })
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        SwingUtilities.invokeLater({
                            field.hidePopup()
                            if (model.onKeyStroke(editor.text))
                                field.showPopup()
                        })
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                    }

                })
    }
}
