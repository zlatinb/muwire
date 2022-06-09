package com.muwire.gui.chat


import com.muwire.gui.UISettings
import com.muwire.gui.contacts.POPLabel
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.ProfileConstants
import sun.swing.UIAction

import javax.swing.*
import javax.swing.event.MenuKeyEvent
import javax.swing.event.MenuKeyListener
import javax.swing.text.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.List
import java.util.stream.Collectors 

class ChatEntryPane extends JTextPane {
    
    private static final char AT = "@".toCharacter()
    private static final char BACKSPACE = (char)8
    private static final int ENTER = 10
    
    private final UISettings settings
    private final List<PersonaOrProfile> members
    
    private JPopupMenu popupMenu
    private Point lastPoint
    private Component lastComponent
    private Action backspaceAction
    
    Closure actionPerformed
    
    ChatEntryPane(UISettings settings, List<PersonaOrProfile> members) {
        super()
        this.settings = settings
        this.members = members
        
        setEditable(true)
        addKeyListener(new KeyAdapter() {
            @Override
            void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == AT) {
                    lastPoint = getCaret().getMagicCaretPosition()
                    lastComponent = e.getComponent()
                    if (lastPoint == null || lastComponent == null)
                        return
                    SwingUtilities.invokeLater {
                        showPopupMenu(false)
                    }
                } else if (((int)e.getKeyChar()) == ENTER) {
                    SwingUtilities.invokeLater {
                       actionPerformed.call()
                    }
                }
            }
        })

        KeyStroke back = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)
        InputMap inputMap = getInputMap()
        ActionMap actionMap = getActionMap()
        
        Object backObject = inputMap.get(back)
        backspaceAction = actionMap.get(backObject)
        actionMap.put(backObject, new BackspaceAction(backspaceAction))
    }

    private String getTextSinceAt(){
        final int caretPosition = getCaret().getDot()
        int startPosition = caretPosition
        while (startPosition > 0) {
            if (getText(startPosition - 1, 1) == "@")
                break
            startPosition --
        }
        getText(startPosition, caretPosition - startPosition)
    }
    
    private void showPopupMenu(boolean filter) {
        popupMenu?.setVisible(false)
        
        String typedText = getTextSinceAt()

        List<JMenuItem> items = members.stream().
                filter({
                    if (!filter)
                        return true
                    justName(it).containsIgnoreCase(typedText)
                }).
                map({new MemberAction(it)}).
                map({new JMenuItem(it)}).
                collect(Collectors.toList())
        if (items.isEmpty())
            return
        
        popupMenu = new JPopupMenu()
        items.each {popupMenu.add(it)}
        
        popupMenu.addMenuKeyListener(new MenuKeyListener() {
            @Override
            void menuKeyTyped(MenuKeyEvent e) {
                char keyChar = e.getKeyChar()
                if (((int)keyChar) == ENTER)
                    return
                if (keyChar == BACKSPACE) {
                    backspaceAction.actionPerformed(new ActionEvent(ChatEntryPane.this, 0, null))
                    SwingUtilities.invokeLater {
                        showPopupMenu(true)
                    }
                    return
                }
                Document document = getDocument()
                int position = getCaret().getDot()
                document.insertString(position, "${e.getKeyChar()}", null)
                SwingUtilities.invokeLater {
                    showPopupMenu(true)
                }
            }

            @Override
            void menuKeyPressed(MenuKeyEvent e) {
            }

            @Override
            void menuKeyReleased(MenuKeyEvent e) {
            }
        })
        
        popupMenu.show(lastComponent, (int)lastPoint.getX(), (int)lastPoint.getY())
    }
    
    private static String justName(PersonaOrProfile pop) {
        String name = pop.getPersona().getHumanReadableName()
        name.substring(0, name.indexOf("@"))
    }
    
    private class MemberAction extends AbstractAction {
        private final PersonaOrProfile personaOrProfile
        MemberAction(PersonaOrProfile personaOrProfile) {
            this.personaOrProfile = personaOrProfile
            putValue(SMALL_ICON, personaOrProfile.getThumbnail())
            putValue(NAME, personaOrProfile.getPersona().getHumanReadableName())
        }

        @Override
        void actionPerformed(ActionEvent e) {
            final int position = getCaret().getDot()
            int startPosition = position
            while(startPosition > 0) {
                if (getText(startPosition - 1, 1) == "@")
                    break
                startPosition--
            }
            startPosition = Math.max(startPosition, 0)
            setSelectionStart(startPosition)
            setSelectionEnd(position)
            replaceSelection("")

            final String name = personaOrProfile.getPersona().getHumanReadableName()
            
            StyledDocument document = getStyledDocument()
            def popLabel = new POPLabel(personaOrProfile, settings)
            popLabel.setMaximumSize([200, ProfileConstants.MAX_THUMBNAIL_SIZE] as Dimension)
            Style style = document.addStyle("newStyle", null)
            StyleConstants.setComponent(style, popLabel)
            document.insertString(startPosition, name, style)
            
            popupMenu?.setVisible(false)
            popupMenu = null
        }
    }
    
    private class BackspaceAction extends UIAction {
        private final Action delegate
        BackspaceAction(Action delegate) {
            super("backspace")
            this.delegate = delegate
        }

        @Override
        void actionPerformed(ActionEvent e) {
            StyledDocument document = getStyledDocument()
            int position = getCaret().getDot() - 1
            Element element = document.getCharacterElement(position)
            if (element.getAttributes().getAttribute(StyleConstants.ComponentAttribute) == null) {
                delegate.actionPerformed(e)
                return
            }
            while(position > 0) {
                element = document.getCharacterElement(position)
                if (element.getAttributes().getAttribute(StyleConstants.ComponentAttribute) == null)
                    break
                delegate.actionPerformed(e)
                position--
            }
        }
    }
}
