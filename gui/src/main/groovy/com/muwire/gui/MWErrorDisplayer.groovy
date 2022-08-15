package com.muwire.gui

import com.muwire.core.Core
import griffon.core.GriffonApplication
import groovy.swing.SwingBuilder

import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JPanel
import java.awt.BorderLayout

import static com.muwire.gui.Translator.trans

class MWErrorDisplayer {
    
    static void fatal(GriffonApplication application, Throwable throwable) {
        fatalDialog(application, throwable, "MUWIRE_ERROR_HEADER", "MUWIRE_ERROR_BODY")
    }
    
    static void fatalInit(GriffonApplication application, Throwable throwable) {
        fatalDialog(application, throwable, "CORE_INIT_ERROR_HEADER", "CORE_INIT_ERROR_BODY_EMBEDDED")
    }
    
    private static void fatalDialog(GriffonApplication application, Throwable throwable, 
                                    String titleKey, String bodyKey) {
        
        Core core = application.context.get("core")
        if (core != null && core.getShutdown().get())
            return
        
        String body = trans(bodyKey)
        String header = trans(titleKey)

        def baos = new ByteArrayOutputStream()
        def pos = new PrintStream(baos)
        throwable.printStackTrace(pos)
        pos.close()
        String trace = new String(baos.toByteArray())

        JDialog dialog = new JDialog()
        JButton quit, copyToClipboard
        def builder = new SwingBuilder()
        JPanel contentPanel = builder.panel {
            borderLayout()
            panel(constraints: BorderLayout.NORTH) {
                label(text: body)
            }
            scrollPane(constraints: BorderLayout.CENTER) {
                textArea(editable: false, lineWrap: false, text: trace)
            }
            panel(constraints: BorderLayout.SOUTH) {
                copyToClipboard = button(text: trans("COPY_TO_CLIPBOARD"))
                quit = button(text: trans("EXIT"))
            }
        }

        quit.addActionListener({
            dialog.setVisible(false)
            application.shutdown()
        })

        copyToClipboard.addActionListener({
            CopyPasteSupport.copyToClipboard(trace)
        })

        dialog.with {
            setModal(true)
            setLocationRelativeTo(null)
            setTitle(header)
            getContentPane().add(contentPanel)
            pack()
            setVisible(true)
        }
    }
}
