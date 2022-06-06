package com.muwire.gui.profile;

import com.muwire.core.Persona;
import com.muwire.core.profile.MWProfile;
import com.muwire.core.profile.MWProfileHeader;
import com.muwire.gui.HTMLSanitizer;

import javax.swing.*;

public interface PersonaOrProfile {
    default Persona getPersona() {return getHeader().getPersona();}
    
    Icon getThumbnail();
    
    default String getTitle() { 
        return HTMLSanitizer.sanitize(getRawTitle());
    }
    
    default String getRawTitle() {
        MWProfileHeader header = getHeader();
        if (header == null)
            return null;
        return header.getTitle();
    }
    
    default MWProfileHeader getHeader() {
        MWProfile profile = getProfile();
        if (profile == null)
            return null;
        return profile.getHeader();
    }
    
    
    default MWProfile getProfile() { return null; }
}
