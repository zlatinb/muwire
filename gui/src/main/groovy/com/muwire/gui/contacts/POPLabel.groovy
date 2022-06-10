package com.muwire.gui.contacts

import com.muwire.core.profile.MWProfileHeader
import com.muwire.gui.HTMLSanitizer
import com.muwire.gui.PersonaCellRenderer
import com.muwire.gui.UISettings
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.ProfileConstants

import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.border.Border
import java.awt.Dimension
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Insets

import static com.muwire.gui.Translator.trans

class POPLabel extends JLabel {
    final PersonaOrProfile personaOrProfile
    private final UISettings settings

    POPLabel(PersonaOrProfile personaOrProfile, UISettings settings) {
        this(personaOrProfile, settings, 
                BorderFactory.createEmptyBorder(2, 2, 2, 2),
                CENTER)
    }
    
    POPLabel(PersonaOrProfile personaOrProfile, UISettings settings, 
             Border border, int verticalAllignment) {
        super()
        setMinimumSize([0, 0] as Dimension)
        setVerticalAlignment(verticalAllignment)
        this.personaOrProfile = personaOrProfile
        this.settings = settings

        int preferredX = 0, preferredY = 0
        
        preferredY = settings.getFontSize()
        if (border != null)
            setBorder(border)
        
        MWProfileHeader header = personaOrProfile.getHeader()
        if (settings.personaRendererAvatars) {
            setIcon(personaOrProfile.getThumbnail())
            preferredY = Math.max(preferredY, ProfileConstants.MAX_THUMBNAIL_SIZE)
            preferredX = ProfileConstants.MAX_THUMBNAIL_SIZE
        }
        
        String text, visibleText
        if (settings.personaRendererIds) {
            text = "<html>${PersonaCellRenderer.htmlize(personaOrProfile.getPersona())}</html>"
            visibleText = personaOrProfile.getPersona().getHumanReadableName()
        } else {
            text = PersonaCellRenderer.justName(personaOrProfile.getPersona())
            visibleText = text
        }
        setText(text)

        FontMetrics fontMetrics = getFontMetrics(getFont())
        for (int i = 0; i < visibleText.length(); i++)
            preferredX += fontMetrics.charWidth(text.charAt(i))

        preferredX += getIconTextGap()
        Insets insets = border.getBorderInsets(this)
        preferredX += insets.left
        preferredX += insets.right
        
        if (personaOrProfile.getTitle() != null) {
            if (settings.personaRendererIds)
                setToolTipText(personaOrProfile.getTitle())
            else {
                String escaped = HTMLSanitizer.escape(personaOrProfile.getRawTitle());
                String tooltip = "<html><body>${personaOrProfile.getPersona().getHumanReadableName()}: ${escaped}</body><html>"
                setToolTipText(tooltip)
            }
        } else
            setToolTipText(trans("NO_PROFILE"))
        
        setMaximumSize([preferredX, preferredY] as Dimension)
    }
}
