package com.muwire.gui

import com.muwire.core.Persona
import com.muwire.core.messenger.MWMessage
import com.muwire.core.profile.MWProfileHeader
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.ThumbnailIcon

import javax.swing.Icon

class MWMessageStatus implements PersonaOrProfile {
    final MWMessage message
    private final MWProfileHeader profileHeader
    private boolean status
    private Icon icon
    MWMessageStatus(MWMessage message, boolean status, MWProfileHeader profileHeader) {
        this.message = message
        this.status = status
        this.profileHeader = profileHeader
    }

    int hashCode() {
        message.hashCode()
    }

    boolean equals(Object o) {
        MWMessageStatus other = (MWMessageStatus) o
        message.equals(other.message)
    }

    @Override
    Persona getPersona() {
        message.getSender()
    }

    @Override
    Icon getThumbnail() {
        if (profileHeader == null)
            return null
        if (icon == null)
            icon = new ThumbnailIcon(profileHeader.getThumbNail())
        return icon
    }

    @Override
    MWProfileHeader getHeader() {
        profileHeader
    }
}
