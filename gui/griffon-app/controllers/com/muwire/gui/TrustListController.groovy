package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.profile.MWProfileHeader
import com.muwire.gui.profile.TrustPOP
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
        
        Persona persona = model.contacts[selectedRow].persona
        MWProfileHeader profileHeader = model.contacts[selectedRow].getHeader()
        UUID uuid = UUID.randomUUID()
        
        def params = [:]
        params.persona = persona
        params.core = core
        params.uuid = uuid
        params.profileHeader = profileHeader
        
        mvcGroup.createMVCGroup("view-profile", uuid.toString(), params)
    }
    
    @ControllerAction
    void close() {
        view.window.setVisible(false)
        mvcGroup.destroy()
    }
}