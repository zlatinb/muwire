package com.muwire.gui.chat

import javax.swing.JTextField
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.Document
import javax.swing.text.PlainDocument

class ChatTextField extends JTextField {
    ChatTextField() {
        setEditable(true)
    }
    
    protected Document createDefaultModel() {
        return new ChatDocument()
    }
    
    private static class ChatDocument extends PlainDocument {
        
        @Override
        void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            getDocumentProperties().put("filterNewlines", false)
            super.insertString(offs, str, a)
        }

        @Override
        String getText(int offset, int length) throws BadLocationException {
            String rv = super.getText(offset, length)
            return rv
        }
    }
}
