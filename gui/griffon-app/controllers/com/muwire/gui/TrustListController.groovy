package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

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
    
    EventBus eventBus
    
    @ControllerAction
    void trustFromTrusted() {
        int selectedRow = view.getSelectedRow("trusted-table")
        if (selectedRow < 0)
            return
        Persona p = model.trusted[selectedRow]
        eventBus.publish(new TrustEvent(persona : p, level : TrustLevel.TRUSTED))
        view.fireUpdate("trusted-table")
    }
    
    @ControllerAction
    void trustFromDistrusted() {
        int selectedRow = view.getSelectedRow("distrusted-table")
        if (selectedRow < 0)
            return
        Persona p = model.distrusted[selectedRow]
        eventBus.publish(new TrustEvent(persona : p, level : TrustLevel.TRUSTED))
        view.fireUpdate("distrusted-table")
    }
    
    @ControllerAction
    void distrustFromTrusted() {
        int selectedRow = view.getSelectedRow("trusted-table")
        if (selectedRow < 0)
            return
        Persona p = model.trusted[selectedRow]
        eventBus.publish(new TrustEvent(persona : p, level : TrustLevel.DISTRUSTED))
        view.fireUpdate("trusted-table")
    }
    
    @ControllerAction
    void distrustFromDistrusted() {
        int selectedRow = view.getSelectedRow("distrusted-table")
        if (selectedRow < 0)
            return
        Persona p = model.distrusted[selectedRow]
        eventBus.publish(new TrustEvent(persona : p, level : TrustLevel.DISTRUSTED))
        view.fireUpdate("distrusted-table")
    }
}