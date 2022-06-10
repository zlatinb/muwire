package com.muwire.gui.chat

import com.muwire.core.Constants
import com.muwire.gui.UISettings
import com.muwire.gui.contacts.POPLabel
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.ProfileConstants

import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JTextPane
import javax.swing.border.Border
import javax.swing.text.Document
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import java.awt.Dimension
import java.text.SimpleDateFormat
import java.util.function.Function

class ChatEntry extends JTextPane {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM hh:mm:ss")

    private static final char AT = "@".toCharacter()
    
    private final UISettings settings
    private final Function<String, PersonaOrProfile> function
    
    private final List<ChatToken> tokens = []
    
    private String currentName
    
    ChatEntry(String text, UISettings settings, Function<String, PersonaOrProfile> function,
        long timestamp, PersonaOrProfile sender) {
        super()
        this.settings = settings
        this.function = function
        setEditable(false)
        setAlignmentY(0f)

        SimpleAttributeSet sab = new SimpleAttributeSet()
        StyleConstants.setAlignment(sab, StyleConstants.ALIGN_LEFT)
        StyleConstants.setSpaceAbove(sab, 0)
        setParagraphAttributes(sab, false)
        
        
        StyledDocument doc = getStyledDocument()
        doc.insertString(doc.getEndPosition().getOffset() - 1, SDF.format(new Date(timestamp)) + " ", null)

        Border border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
        def label = new POPLabel(sender, settings, border, JLabel.TOP)
        def style = doc.addStyle("newStyle", null)
        StyleConstants.setComponent(style, label)
        doc.insertString(doc.getEndPosition().getOffset() - 1, " ", style)
        doc.insertString(doc.getEndPosition().getOffset() - 1, ": ", null)
        
        
        ParsingState state = new TextParsingState()
        for(int i = 0; i < text.length(); i ++)
            state = state.process(text.charAt(i))
        state.finishUp()
        
        tokens.each {it.render()}
    }
    
    private abstract class ParsingState {
        protected final StringBuilder stringBuilder = new StringBuilder()
        protected final int maxSize
        
        protected boolean consumed
        
        ParsingState(int maxSize) {
            this.maxSize = maxSize
        }
        
        ParsingState process(char c) {
            stringBuilder.append(c)
            if (stringBuilder.size() > maxSize) {
                consumed = true
                tokens << new TextChatToken(stringBuilder.toString())
                return new TextParsingState()
            }
            return consume(c)
        }
        
        void finishUp() {
            if (consumed)
                return
            if (stringBuilder.size() > 0)
                tokens << new TextChatToken(stringBuilder.toString())
        }
        
        abstract ParsingState consume(char c)
    }
    
    private class TextParsingState extends ParsingState {
        TextParsingState() {
            super(Integer.MAX_VALUE)
        }
        @Override
        ParsingState consume(char c) {
            if (c == AT) {
                consumed = true
                tokens << new TextChatToken(stringBuilder.toString())
                return new NameParsingState()
            }
            this
        }
    }
    
    private class NameParsingState extends ParsingState {
        NameParsingState() {
            super(Constants.MAX_NICKNAME_LENGTH)
        }

        @Override
        ParsingState consume(char c) {
            if (c == AT) {
                consumed = true
                currentName = stringBuilder.toString()
                return new KeyParsingState()
            }
            this
        }
    }
    
    private class KeyParsingState extends ParsingState {
        KeyParsingState() {
            super(32)
        }

        @Override
        ParsingState consume(char c) {
            if (stringBuilder.length() == maxSize) {
                consumed = true
                String readableName = "${currentName}${stringBuilder.toString()}"
                PersonaOrProfile pop = function.apply(readableName)
                if (pop != null)
                    tokens << new POPChatToken(pop)
                else
                    tokens << new TextChatToken(readableName)
                return new TextParsingState()
            }
            return this
        }
    }
    
    private interface ChatToken {
        void render()
    }
    
    private class TextChatToken implements ChatToken {
        private final String text
        TextChatToken(String text) {
            this.text = text
        }
        
        void render() {
            Document document = getDocument()
            document.insertString(document.getEndPosition().getOffset() - 1, text, null)
        }
    }
    
    private class POPChatToken implements ChatToken {
        private final PersonaOrProfile personaOrProfile
        POPChatToken(PersonaOrProfile personaOrProfile) {
            this.personaOrProfile = personaOrProfile
        }
        
        void render() {
            StyledDocument document = getStyledDocument()

            Border border = BorderFactory.createEtchedBorder()
            def popLabel = new POPLabel(personaOrProfile, settings, border, JLabel.CENTER)
            def style = document.addStyle("newStyle", null)
            StyleConstants.setComponent(style, popLabel)
            document.insertString(document.getEndPosition().getOffset() - 1,
                    personaOrProfile.getPersona().getHumanReadableName(),
                    style)
        }
    }
}
