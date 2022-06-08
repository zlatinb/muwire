package com.muwire.gui.contacts

import com.muwire.gui.UISettings
import sun.swing.UIAction

import javax.swing.Action
import javax.swing.JTextField
import javax.swing.JTextPane
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.basic.BasicComboBoxEditor
import javax.swing.plaf.basic.BasicComboBoxUI
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.FocusListener
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeListener

class ContactChooserEditor extends BasicComboBoxEditor{
    
    private final ContactChooserModel model
    private final ContactChooser field
    final ContactChooserTextPane textPane
    private final UISettings settings
    
    private final List<ActionListener> actionListeners = []
    
    private ContactChooserPOP currentPOP
    
    ContactChooserEditor(ContactChooserModel model, ContactChooser field, UISettings settings) {
        super()
        this.model = model
        this.field = field
        this.settings = settings
        this.textPane = new ContactChooserTextPane(settings)

        textPane.addKeyListener(new KeyAdapter() {
            @Override
            void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode()
                if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN)
                    return
                
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    field.hidePopup()
                    return
                }
                
                SwingUtilities.invokeLater {
                    field.hidePopup()
                    if (model.onKeyStroke(textPane.getLatestText()))
                        field.showPopup()
                }
            }
        })
        
        
        KeyStroke down = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)
        KeyStroke up = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        
        def inputMap = textPane.getInputMap()
        def actionMap = textPane.getActionMap()
        Object downKey = inputMap.get(down)
        Action downAction = actionMap.get(downKey)
        actionMap.put(downKey, new MyAction("selectNext", true, downAction))
        
        Object upKey = inputMap.get(up)
        Action upAction = actionMap.get(upKey)
        actionMap.put(upKey, new MyAction("selectPrevious", false, upAction))
        
        Object enterKey = inputMap.get(enter)
        actionMap.put(enterKey, new EnterAction())
     
    }
    
    public void setItem(Object o) {
        if (o == null || o instanceof ContactChooserPOP)
            currentPOP = o
        else
            throw new Exception("setItem $o with class ${o.getClass().getName()}")
        if (currentPOP?.getPersona() != null)
            textPane.appendPersonaName(currentPOP.getPersona())
    }
    
    public Object getItem() {
        currentPOP
    }

    @Override
    public Component getEditorComponent() {
        textPane
    }
    
    public void addActionListener(ActionListener actionListener) {
        textPane.addKeyListener(new EventAdapter(actionListener))
    }
    
    public void removeActionListener(ActionListener actionListener)  {
        actionListeners.remove(actionListener)
    }
    
    private class EventAdapter implements KeyListener {
        
        private final ActionListener delegate
        EventAdapter(ActionListener delegate) {
            this.delegate = delegate
        }

        @Override
        void keyTyped(KeyEvent e) {}

        @Override
        void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode()
            if (keyCode == KeyEvent.VK_ENTER) {
                ActionEvent toForward = new ActionEvent(textPane, e.getID(), "")
                delegate.actionPerformed(toForward)
            }
        }

        @Override
        void keyReleased(KeyEvent e) {}
    } 
    
    private class MyAction extends UIAction {
        
        private final Action delegate
        private final boolean down

        MyAction(String name, boolean down, Action delegate) {
            super(name)
            this.down = down
            this.delegate = delegate
        }

        @Override
        void actionPerformed(ActionEvent e) {
            if (field.isShowing() && textPane.isCarretAtEnd()) {
                if (field.isPopupVisible()) {
                    if (down)
                        field.getUI().selectNextPossibleValue()
                    else
                        field.getUI().selectPreviousPossibleValue()
                }
                else
                    field.setPopupVisible(true)
            }
            delegate.actionPerformed(e)
        }
    }
    
    private class EnterAction extends UIAction {
        EnterAction() {
            super("Enter")
        }
        
        @Override
        void actionPerformed(ActionEvent event) {
            def selected = model.getSelectedPOP()
            if (selected == null)
                return
            textPane.insertPOP(selected)
        }
    }
}
