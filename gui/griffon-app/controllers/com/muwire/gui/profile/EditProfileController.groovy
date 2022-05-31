package com.muwire.gui.profile

import com.muwire.core.Constants
import com.muwire.core.profile.MWProfile
import com.muwire.core.profile.MWProfileHeader
import com.muwire.core.profile.MWProfileImageFormat
import com.muwire.gui.CopyPasteSupport
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.imageio.ImageIO
import javax.swing.JOptionPane

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
        if (model.imageData == null || model.thumbnailData == null) {
            view.showErrorNoImage()
            return
        }
        String title = view.titleField.getText()
        if (title == null)
            title = ""
        if (title.length() > Constants.MAX_PROFILE_TITLE_LENGTH) {
            view.showErrorLongTitle()
            return
        }
        String body = view.bodyArea.getText()
        if (body == null)
            body = ""
        if (body.length() > Constants.MAX_COMMENT_LENGTH) {
            view.showErrorLongProfile()
            return
        }

        MWProfileHeader header = new MWProfileHeader(model.core.me, model.thumbnailData, title, model.core.spk)
        MWProfile profile = new MWProfile(header, model.imageData, model.format, body, model.core.spk)
        model.core.myProfile = profile
        model.core.saveProfile()
        cancel()
    }
    
    @ControllerAction
    void cancel() {
        view.dialog.setVisible(false)
    }
}
