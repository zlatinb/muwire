package com.muwire.gui.chat

import com.muwire.gui.UISettings
import com.muwire.gui.mulinks.FileMuLink

import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.Image
import java.util.function.Consumer

class FileLinkPanel extends JPanel {
    
    private static final Icon DOWN_ICON
    static {
        DOWN_ICON = new ImageIcon(FileLinkPanel.class.getClassLoader().getResource("down_arrow.png"))
    }
    
    private final FileMuLink link
    private final UISettings settings
    private final Consumer<FileMuLink> linkConsumer
    
    FileLinkPanel(FileMuLink link, UISettings settings, Consumer<FileMuLink> linkConsumer) {
        super()
        this.linkConsumer = linkConsumer
        
        setLayout(new BorderLayout())
        
        def label = new FileLinkLabel(link, settings, false)
        add(label, BorderLayout.CENTER)
        
        JButton button = new JButton()
        button.setIcon(DOWN_ICON)
        button.addActionListener({linkConsumer.accept(link)})
        add(button, BorderLayout.EAST)
    }
}
