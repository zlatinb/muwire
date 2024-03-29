package com.muwire.gui

import griffon.core.artifact.GriffonView

import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

import static com.muwire.gui.Translator.trans

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JTextArea
import javax.swing.SwingConstants
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class AddContactView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    AddContactModel model

    def mainFrame
    def dialog
    def p
    JTextArea idArea
    JTextArea reasonArea
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, trans("ADD_CONTACT_TITLE"), true)
        dialog.setResizable(false)
        
        p = builder.panel {
            gridLayout(rows : 2, cols : 1)
            panel {
                borderLayout()
                panel(constraints : BorderLayout.NORTH) {
                    label(text : trans("ADD_CONTACT_BODY"))
                }
                scrollPane(constraints : BorderLayout.CENTER) {
                    idArea = textArea(editable : true, lineWrap : true, wrapStyleWord : true)
                }
            }
            panel {
                borderLayout()
                panel(constraints : BorderLayout.NORTH) {
                    label(text : trans("ENTER_REASON_OPTIONAL"))
                }
                scrollPane(constraints : BorderLayout.CENTER) {
                    reasonArea = textArea(editable : true, lineWrap : true, wrapStyleWord : true)
                }
                panel(constraints : BorderLayout.SOUTH) {
                    buttonGroup(id : "trustLevel")
                    radioButton(text : trans("TRUSTED"), selected : bind {model.trusted}, buttonGroup : trustLevel, actionPerformed : setTrusted )
                    radioButton(text : trans("DISTRUSTED"), selected : bind {!model.trusted}, buttonGroup: trustLevel, actionPerformed : setDistrusted )
                    button(text : trans("ADD_CONTACT_SPECIFIC"), addAction)
                    button(text : trans("CANCEL"), cancelAction)
                }
            }
        }
        
        idArea.addMouseListener(new MouseAdapter() {
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
        idArea.setText(contents)
    }
    
    void mvcGroupInit(Map<String,String> args) {
        dialog.getContentPane().add(p)
        dialog.pack()
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.show()
    }
    
    def setTrusted = {
        model.trusted = true
    }
    
    def setDistrusted = {
        model.trusted = false
    }
}