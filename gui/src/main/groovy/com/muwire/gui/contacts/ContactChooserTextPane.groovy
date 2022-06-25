package com.muwire.gui.contacts

import com.muwire.core.Persona
import com.muwire.gui.UISettings
import com.muwire.gui.profile.PersonaOrProfile

import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.AttributeSet
import javax.swing.text.Document
import javax.swing.text.Element
import javax.swing.text.Style
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

class ContactChooserTextPane extends JTextPane {
    private static final String SEPARATOR_STRING = ";"
    private final UISettings settings
    
    ContactChooserTextPane(UISettings settings) {
        super()
        this.settings = settings
        setEditable(true)
    }
    
    String getLatestText() {
        String allText = getText()
        int pos = getStartOffset()
        allText = allText.substring(pos).trim()
        if (allText.endsWith(SEPARATOR_STRING))
            allText = allText.substring(0, allText.length() - SEPARATOR_STRING.length())
        allText
    }
    
    private int getStartOffset() {
        String allText = getText()
        int lastIndex = allText.lastIndexOf(SEPARATOR_STRING)
        if (lastIndex < 0)
            return 0
        lastIndex + SEPARATOR_STRING.length()
    }
    void appendPersonaName(Persona persona) {
        Document document = getDocument()
        int startOffset = getStartOffset()
        setSelectionStart(startOffset)
        setSelectionEnd(document.getEndPosition().getOffset())
        String name = persona.getHumanReadableName()
        replaceSelection(name)
        int end = document.getEndPosition().getOffset() - 1
        setCaretPosition(end)
    }
    
    void insertPOP(PersonaOrProfile personaOrProfile) {
        
        StyledDocument doc = getDocument()
        def popLabel = new POPLabel(personaOrProfile, settings)
        
        int startOffset = getStartOffset()
        setSelectionStart(startOffset)
        setSelectionEnd(doc.getEndPosition().getOffset() - 1)
        replaceSelection("")

        Style style = doc.addStyle("newStyle", null)
        StyleConstants.setComponent(style, popLabel)
        doc.insertString(startOffset, SEPARATOR_STRING, style)
        
        setCaretPosition(doc.getEndPosition().getOffset() - 1)
    }
    
    Set<PersonaOrProfile> getSelectedPOPs() {
        Set<PersonaOrProfile> rv = new HashSet<>()
        for (int i = 0; i < getDocument().getEndPosition().getOffset(); i++) {
            Element element = getStyledDocument().getCharacterElement(i)
            def panel = element.getAttributes().getAttribute(StyleConstants.ComponentAttribute)
            if (panel == null)
                continue
            rv.add(panel.personaOrProfile)
        }
        rv
    }
    
    boolean isCarretAtEnd() {
        int position = getCaret().getDot()
        position >= getStartOffset()
    }
}
