package com.muwire.gui.profile

import com.muwire.gui.CopyPasteSupport
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class EditProfileController {
    @MVCMember @Nonnull
    EditProfileModel model
    @MVCMember @Nonnull
    EditProfileView view
    
    @ControllerAction
    void copyShort() {
        CopyPasteSupport.copyToClipboard(model.core.me.getHumanReadableName())
    }
    
    @ControllerAction
    void copyFull() {
        CopyPasteSupport.copyToClipboard(model.core.me.toBase64())
    }
}
