package com.muwire.gui.chat

import javax.swing.JTextArea
import javax.swing.JTextField

class ChatEntry extends JTextArea {
    
    ChatEntry(String text) {
        setEditable(false)
        setLineWrap(true)
        setWrapStyleWord(true)
        setColumns(countRows(text))
        setText(text)
        setAlignmentY(0f)
    }
    
    private static int countRows(String text) {
        int rv = 0
        char newLine = "\n".toCharacter()
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i)
            if (c == newLine)
                rv++
        }
        rv
    }
}
