package com.muwire.gui.chat

import com.muwire.gui.CopyPasteSupport
import com.muwire.gui.UISettings
import com.muwire.gui.mulinks.CollectionMuLink
import com.muwire.gui.mulinks.FileMuLink
import com.muwire.gui.mulinks.MuLink

import static com.muwire.gui.Translator.trans

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
    
    private static final Icon DOWN_ICON, COPY_ICON
    static {
        DOWN_ICON = new ImageIcon(MuLinkPanel.class.getClassLoader().getResource("down_arrow.png"))
        COPY_ICON = new ImageIcon(MuLinkPanel.class.getClassLoader().getResource("copy.png"))
    }
    
    private final UISettings settings
    private final Consumer<MuLink> linkConsumer

    MuLinkPanel(MuLink link, UISettings settings, Consumer<MuLink> linkConsumer) {
        super()
        this.settings = settings
        this.linkConsumer = linkConsumer
        
        setLayout(new BorderLayout())

        JPanel buttonPanel = new JPanel()
        add(buttonPanel, BorderLayout.EAST)
        
        JButton downloadButton = new JButton()
        downloadButton.setIcon(DOWN_ICON)
        downloadButton.addActionListener({linkConsumer.accept(link)})
        buttonPanel.add(downloadButton)
        
        JButton copyButton = new JButton()
        copyButton.setIcon(COPY_ICON)
        copyButton.addActionListener({ CopyPasteSupport.copyToClipboard(link.toLink())})
        copyButton.setToolTipText(trans("COPY_LINK_TO_CLIPBOARD"))
        buttonPanel.add(copyButton)
        
        def label = null
        if (link.getLinkType() == MuLink.LinkType.FILE) {
            label = new FileLinkLabel((FileMuLink) link, settings, false)
            downloadButton.setToolTipText(trans("TOOLTIP_MULINK_PANEL_DOWNLOAD"))
        } else if (link.getLinkType() == MuLink.LinkType.COLLECTION) {
            label = new CollectionLinkLabel((CollectionMuLink) link, settings, false)
            downloadButton.setToolTipText(trans("TOOLTIP_MULINK_PANEL_VIEW"))
        }
        label.setToolTipText(trans("TOOLTIP_MULINK_PANEL", link.getHost().getHumanReadableName()))
        add(label, BorderLayout.CENTER)
        
        
        
        def labelDim = label.getMaximumSize()
        double preferredY = labelDim.getHeight()
        double preferredX = labelDim.getWidth() + DOWN_ICON.getIconWidth() + COPY_ICON.getIconWidth()

        Border border = BorderFactory.createEtchedBorder()
        setBorder(border)
        Insets insets = border.getBorderInsets(this)
        preferredX += insets.left
        preferredX += insets.right
        preferredY += insets.top
        preferredY += insets.bottom
        
        preferredX += 40
        
        setMaximumSize([(int)preferredX, (int)preferredY] as Dimension)
        float alignmentY = 0.5f + (settings.fontSize * 1f / preferredY) / 2
        setAlignmentY(alignmentY)
    }
}
