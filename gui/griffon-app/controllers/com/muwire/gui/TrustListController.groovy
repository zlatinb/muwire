package com.muwire.gui

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

    EventBus eventBus

    @ControllerAction
    void trustFromTrusted() {
        int selectedRow = view.getSelectedRow("trusted-table")
        if (selectedRow < 0)
            return
        String reason = JOptionPane.showInputDialog("Enter reason (optional)")
        Persona p = model.trusted[selectedRow]
        eventBus.publish(new TrustEvent(persona : p, level : TrustLevel.TRUSTED, reason : reason))
        view.fireUpdate("trusted-table")
    }

    @ControllerAction
    void trustFromDistrusted() {
        int selectedRow = view.getSelectedRow("distrusted-table")
        if (selectedRow < 0)
            return
        String reason = JOptionPane.showInputDialog("Enter reason (optional)")
        Persona p = model.distrusted[selectedRow]
        eventBus.publish(new TrustEvent(persona : p, level : TrustLevel.TRUSTED, reason : reason))
        view.fireUpdate("distrusted-table")
    }

    @ControllerAction
    void distrustFromTrusted() {
        int selectedRow = view.getSelectedRow("trusted-table")
        if (selectedRow < 0)
            return
        String reason = JOptionPane.showInputDialog("Enter reason (optional)")
        Persona p = model.trusted[selectedRow]
        eventBus.publish(new TrustEvent(persona : p, level : TrustLevel.DISTRUSTED, reason : reason))
        view.fireUpdate("trusted-table")
    }

    @ControllerAction
    void distrustFromDistrusted() {
        int selectedRow = view.getSelectedRow("distrusted-table")
        if (selectedRow < 0)
            return
        String reason = JOptionPane.showInputDialog("Enter reason (optional)")
        Persona p = model.distrusted[selectedRow]
        eventBus.publish(new TrustEvent(persona : p, level : TrustLevel.DISTRUSTED, reason : reason))
        view.fireUpdate("distrusted-table")
    }
}