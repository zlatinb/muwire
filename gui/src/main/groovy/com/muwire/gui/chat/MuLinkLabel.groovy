package com.muwire.gui.chat

import com.muwire.gui.HTMLSanitizer
import com.muwire.gui.TableUtil
import com.muwire.gui.UISettings
import com.muwire.core.mulinks.MuLink

import javax.swing.*
import javax.swing.border.Border
import java.awt.*

abstract class MuLinkLabel extends JLabel {
    final MuLink link

    private final UISettings settings

    MuLinkLabel(MuLink link, UISettings settings, boolean border) {
        super()
        this.link = link
        this.settings = settings
        
        String visibleText = getVisibleText()
        String escaped = HTMLSanitizer.escape(visibleText)
        setText("<html>$escaped</html>")
        
        int preferredX = 0, preferredY = 24

        preferredX += TableUtil.stringWidth(this, visibleText)

        if (border) {
            Border b = BorderFactory.createEtchedBorder()
            setBorder(b)
            Insets insets = b.getBorderInsets(this)
            preferredX += insets.left
            preferredX += insets.right
        }

        setMaximumSize([preferredX, preferredY] as Dimension)
        float alignmentY = 0.5f + (settings.fontSize * 1f / preferredY) / 2
        setAlignmentY(alignmentY)
    }
    
    protected abstract String getVisibleText()
}
