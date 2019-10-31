package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class SystemStatusController {
    @MVCMember @Nonnull
    SystemStatusModel model
    @MVCMember @Nonnull
    SystemStatusView view

    @ControllerAction
    void refresh() {
        
        long totalRam = Runtime.getRuntime().totalMemory()
        long usedRam = totalRam - Runtime.getRuntime().freeMemory()
        
        model.usedRam = usedRam
        model.totalRam = totalRam
        model.maxRam = Runtime.getRuntime().maxMemory()
        model.javaVendor = System.getProperty("java.vendor")
        model.javaVersion = System.getProperty("java.version")
    }
    
    @ControllerAction
    void close() {
        view.dialog.setVisible(false)
        mvcGroup.destroy()
    }
}