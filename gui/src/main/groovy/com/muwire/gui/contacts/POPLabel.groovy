package com.muwire.gui.contacts

import com.muwire.core.profile.MWProfileHeader
import com.muwire.gui.PersonaCellRenderer
import com.muwire.gui.UISettings
import com.muwire.gui.profile.PersonaOrProfile

import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.border.Border

class POPLabel extends JLabel {
    final PersonaOrProfile personaOrProfile
    private final UISettings settings

    POPLabel(PersonaOrProfile personaOrProfile, UISettings settings) {
        super()
        this.personaOrProfile = personaOrProfile
        this.settings = settings

        Border border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        setBorder(border)
        
        MWProfileHeader header = personaOrProfile.getHeader()
        if (settings.personaRendererAvatars) {
            setIcon(personaOrProfile.getThumbnail())
        }
        
        String text
        if (settings.personaRendererIds) {
            text = personaOrProfile.getPersona().getHumanReadableName()
        } else
            text = PersonaCellRenderer.justName(personaOrProfile.getPersona())
        setText(text)
        
        // TODO: add tooltip
    }
}
