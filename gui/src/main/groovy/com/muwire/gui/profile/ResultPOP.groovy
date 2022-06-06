package com.muwire.gui.profile

import com.muwire.core.Persona
import com.muwire.core.profile.MWProfileHeader
import com.muwire.core.search.UIResultEvent
import com.muwire.gui.HTMLSanitizer

import javax.swing.Icon

class ResultPOP implements PersonaOrProfile {
    final UIResultEvent event
    private Icon thumbNail
    ResultPOP(UIResultEvent event) {
        this.event = event
    }

    @Override
    Persona getPersona() {
        return event.sender
    }

    @Override
    Icon getThumbnail() {
        if (event.profileHeader == null)
            return null
        if (thumbNail == null) {
            thumbNail = new ThumbnailIcon(event.profileHeader.getThumbNail())
        }
        thumbNail
    }

    @Override
    MWProfileHeader getHeader() {
        return event.profileHeader
    }
}
