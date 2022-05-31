package com.muwire.gui.profile

import com.muwire.core.profile.MWProfileImageFormat
import com.muwire.gui.CopyPasteSupport
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.imageio.ImageIO

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
    
    @ControllerAction
    void generate() {
        String me = model.core.me.getHumanReadableName()
        def generated = IdenticonGenerator.generateIdenticon(me.substring(me.indexOf("@") + 1))
        model.format = MWProfileImageFormat.PNG
        def baos = new ByteArrayOutputStream()
        ImageIO.write(generated, "png", baos)
        view.setImageAndThumbnail(baos.toByteArray())
    }
    
    @ControllerAction
    void save() {
        // TODO: implement
        cancel()
    }
    
    @ControllerAction
    void cancel() {
        view.dialog.setVisible(false)
    }
}
