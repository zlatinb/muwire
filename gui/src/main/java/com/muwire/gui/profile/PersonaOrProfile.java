package com.muwire.gui.profile;

import com.muwire.core.Persona;

import javax.swing.*;

public interface PersonaOrProfile {
    Persona getPersona();
    Icon getThumbnail();
    String getTitle();
}
