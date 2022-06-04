package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.profile.MWProfileHeader
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.TrustPOP
import com.muwire.gui.profile.ViewProfileHelper
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull
import javax.swing.JOptionPane

import com.muwire.core.EventBus
import com.muwire.core.Persona
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

@ArtifactProviderFor(GriffonController)
class TrustListController {
    @MVCMember @Nonnull
    TrustListModel model
    @MVCMember @Nonnull
    TrustListView view

    Core core
    
    @ControllerAction
    void viewProfile() {
        int selectedRow = view.getSelectedRow()
        if (selectedRow < 0)
            return
        
        PersonaOrProfile pop = model.contacts[selectedRow]
        ViewProfileHelper.initViewProfileGroup(core, mvcGroup, pop)
    }
    
    @ControllerAction
    void close() {
        view.window.setVisible(false)
        mvcGroup.destroy()
    }
}