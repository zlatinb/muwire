package com.muwire.gui.chat


import com.muwire.gui.UISettings
import com.muwire.gui.contacts.POPLabel
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.ProfileConstants
import sun.swing.UIAction

import javax.swing.*
import javax.swing.border.Border
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
    private static final char SPACE = " ".toCharacter()
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
                    if (lastPoint == null)
                        lastPoint = getBounds().getLocation()
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

        InputMap inputMap = getInputMap()
        ActionMap actionMap = getActionMap()
        
        KeyStroke back = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)
        Object backObject = inputMap.get(back)
        backspaceAction = actionMap.get(backObject)
        actionMap.put(backObject, new ForwardingAction(backspaceAction, false, true))
        
        KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)
        Object deleteObject = inputMap.get(delete)
        Action originalDeleteAction = actionMap.get(deleteObject)
        actionMap.put(deleteObject, new ForwardingAction(originalDeleteAction, true, true))
        
        KeyStroke left = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)
        Object leftObject = inputMap.get(left)
        Action originalLeftAction = actionMap.get(leftObject)
        actionMap.put(leftObject, new ForwardingAction(originalLeftAction, false, false))
        
        KeyStroke right = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)
        Object rightObject = inputMap.get(right)
        Action originalRightAction = actionMap.get(rightObject)
        actionMap.put(rightObject, new ForwardingAction(originalRightAction, true, false))
        
        Action noAction = new NullAction()
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        Object enterObject = inputMap.get(enter)
        actionMap.put(enterObject, noAction)
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


            Border border = BorderFactory.createEtchedBorder()
            def popLabel = new POPLabel(personaOrProfile, settings, border, JLabel.CENTER)
            
            StyledDocument document = getStyledDocument()
            Style style = document.addStyle("newStyle", null)
            StyleConstants.setComponent(style, popLabel)
            document.insertString(startPosition, " ", style)
            
            popupMenu?.setVisible(false)
            popupMenu = null
        }
    }
    
    String getFinalText() {
        String currentText = getText()
        StringBuilder sb = new StringBuilder()
        for (int i = 0; i < currentText.length(); i ++) {
            final char c = currentText.charAt(i)
            if ( c != SPACE) {
                sb.append(c)
                continue
            }
            def component = getComponentForPosition(i)
            if (component == null) {
                sb.append(c)
                continue
            }
            POPLabel label = (POPLabel) component
            sb.append(label.personaOrProfile.getPersona().toBase64())
            sb.append(AT)
        }
        sb.toString()
    }

    private boolean isInComponent(int position) {
        getComponentForPosition(position) != null
    }
    
    private Object getComponentForPosition(int position) {
        StyledDocument document = getStyledDocument()
        Element element = document.getCharacterElement(position)
        element.getAttributes().getAttribute(StyleConstants.ComponentAttribute)
    }
    
    private class ForwardingAction extends UIAction {
        private final Action delegate
        private final boolean forward, modifying
        ForwardingAction(Action delegate, boolean forward, boolean modifying) {
            super("forwarding")
            this.delegate = delegate
            this.forward = forward
            this.modifying = modifying
        }

        @Override
        void actionPerformed(ActionEvent e) {
            int position = getCaret().getDot()
            if (!forward)
                position--
            if (!isInComponent(position)) {
                delegate.actionPerformed(e)
                return
            }
            if (forward) {
                while(position < getDocument().getEndPosition().getOffset()) {
                    if (!isInComponent(position))
                        break
                    delegate.actionPerformed(e)
                    if (!modifying)
                        position++
                }
            } else {
                while (position > 0) {
                    if (!isInComponent(position))
                        break
                    delegate.actionPerformed(e)
                    position--
                }
            }
        }
    }
    
    private static class NullAction extends UIAction {
        NullAction() {
            super("nothing")
        }

        @Override
        void actionPerformed(ActionEvent e) {
        }
    }
}
