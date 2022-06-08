package com.muwire.gui.contacts

import com.muwire.core.Persona
import com.muwire.core.profile.MWProfile
import com.muwire.core.profile.MWProfileHeader
import com.muwire.core.trust.TrustService.TrustEntry
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.ThumbnailIcon

import javax.swing.Icon

class ContactChooserPOP implements PersonaOrProfile {
    private final TrustEntry trustEntry
    private final String text
    private Icon icon
    
    ContactChooserPOP(TrustEntry trustEntry) {
        this.trustEntry = trustEntry
        this.text = null
    }
    
    ContactChooserPOP(String text) {
        this.text = text
        this.trustEntry = null
    }

    Persona getPersona() {
        trustEntry?.getPersona()
    }

    @Override
    Icon getThumbnail() {
        MWProfileHeader header = getHeader()
        if (header == null)
            return null
        if (icon == null)
            icon = new ThumbnailIcon(header.getThumbNail())
        return icon
    }

    @Override
    MWProfileHeader getHeader() {
        trustEntry.getProfileHeader()
    }

    @Override
    MWProfile getProfile() {
        trustEntry.getProfile()
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof ContactChooserPOP))
            return false
        ContactChooserPOP other = (ContactChooserPOP)o
        boolean rv = Objects.equals(text, other.text) &&
                Objects.equals(trustEntry, other.trustEntry)
        rv
    }
    
    String toString() {
        if (text != null)
            return text
        else
            return getPersona().getHumanReadableName()
    }
}
