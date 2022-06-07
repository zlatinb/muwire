package com.muwire.gui

import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.basic.BasicComboBoxEditor
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

import static com.muwire.gui.Translator.trans

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
        
        editor.addMouseListener(new MouseAdapter() {
            @Override
            void mousePressed(MouseEvent e) {
                if (!CopyPasteSupport.canPasteString())
                    return
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    showPopupMenu(e)
            }

            @Override
            void mouseReleased(MouseEvent e) {
                if (!CopyPasteSupport.canPasteString())
                    return
                if (e.isPopupTrigger() || e.button == MouseEvent.BUTTON3)
                    showPopupMenu(e)
            }
        })
        
        editor.addKeyListener(new KeyAdapter() {
            @Override
            void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode()
                if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) 
                    return
                SwingUtilities.invokeLater({
                    field.hidePopup()
                    if (model.onKeyStroke(editor.text))
                        field.showPopup()
                })
            }
        })
    }
    
    private void showPopupMenu(MouseEvent event) {
        JPopupMenu menu = new JPopupMenu()
        JMenuItem paste = new JMenuItem(trans("PASTE"))
        paste.addActionListener({doPaste()})
        menu.add(paste)
        menu.show(event.getComponent(), event.getX(), event.getY())
    }
    
    private void doPaste() {
        String contents = CopyPasteSupport.pasteFromClipboard()
        if (contents == null)
            return
        editor.setText(contents)
    }
}
