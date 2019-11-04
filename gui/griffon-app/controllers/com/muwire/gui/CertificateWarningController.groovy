package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class CertificateWarningController {
    @MVCMember @Nonnull
    CertificateWarningView view

    UISettings settings
    File home
    
    @ControllerAction
    void dismiss() {
        if (view.checkbox.model.isSelected()) {
            settings.certificateWarning = false
            File propsFile = new File(home, "gui.properties")
            propsFile.withOutputStream { settings.write(it) }
        }
        view.dialog.setVisible(false)
        mvcGroup.destroy()
    }
}