package com.muwire.gui.chat

import com.muwire.gui.UISettings
import com.muwire.gui.mulinks.CollectionMuLink
import com.muwire.gui.mulinks.FileMuLink
import com.muwire.gui.mulinks.MuLink

import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.border.Border
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Insets
import java.util.function.Consumer

class MuLinkPanel extends JPanel {
    
    private static final Icon DOWN_ICON
    static {
        DOWN_ICON = new ImageIcon(MuLinkPanel.class.getClassLoader().getResource("down_arrow.png"))
    }
    
    private final UISettings settings
    private final Consumer<MuLink> linkConsumer

    MuLinkPanel(MuLink link, UISettings settings, Consumer<MuLink> linkConsumer) {
        super()
        this.settings = settings
        this.linkConsumer = linkConsumer
        
        setLayout(new BorderLayout())
        def label = null
        if (link.getLinkType() == MuLink.LinkType.FILE)
            label = new FileLinkLabel((FileMuLink)link, settings, false)
        else if (link.getLinkType() == MuLink.LinkType.COLLECTION)
            label = new CollectionLinkLabel((CollectionMuLink)link, settings, false)
        add(label, BorderLayout.CENTER)
        
        JButton button = new JButton()
        button.setIcon(DOWN_ICON)
        button.addActionListener({linkConsumer.accept(link)})
        add(button, BorderLayout.EAST)
        
        def labelDim = label.getMaximumSize()
        double preferredY = labelDim.getHeight()
        double preferredX = labelDim.getWidth() + DOWN_ICON.getIconWidth()

        Border border = BorderFactory.createEtchedBorder()
        setBorder(border)
        Insets insets = border.getBorderInsets(this)
        preferredX += insets.left
        preferredX += insets.right
        preferredY += insets.top
        preferredY += insets.bottom
        
        preferredX += 20
        
        setMaximumSize([(int)preferredX, (int)preferredY] as Dimension)
        float alignmentY = 0.5f + (settings.fontSize * 1f / preferredY) / 2
        setAlignmentY(alignmentY)
    }
}
