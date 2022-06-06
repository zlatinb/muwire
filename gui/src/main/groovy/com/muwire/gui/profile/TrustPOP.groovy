package com.muwire.gui.profile

import com.muwire.core.Persona
import com.muwire.core.profile.MWProfile
import com.muwire.core.profile.MWProfileHeader
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService.TrustEntry
import com.muwire.gui.HTMLSanitizer

import javax.swing.Icon

class TrustPOP implements PersonaOrProfile {
    private final TrustEntry trustEntry
    private Icon icon
    TrustPOP(TrustEntry trustEntry) {
        this.trustEntry = trustEntry
    }
    
    public String getReason() {
        return trustEntry.getReason()
    }
    
    @Override
    Persona getPersona() {
        return trustEntry.getPersona()
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
    public MWProfileHeader getHeader() {
        trustEntry.getProfileHeader()
    }
    
    @Override
    public MWProfile getProfile() {
        trustEntry.getProfile()
    }
}
