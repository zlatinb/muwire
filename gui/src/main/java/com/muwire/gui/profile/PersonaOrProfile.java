package com.muwire.gui.profile;

import com.muwire.core.Persona;
import com.muwire.core.profile.MWProfile;
import com.muwire.core.profile.MWProfileHeader;

import javax.swing.*;

public interface PersonaOrProfile {
    Persona getPersona();
    Icon getThumbnail();
    String getTitle();
    
    default MWProfileHeader getHeader() {
        return null;
    }
    default MWProfile getProfile() { return null; }
}
