package com.muwire.gui.profile

import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel
import com.muwire.gui.CopyPasteSupport
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import static com.muwire.gui.Translator.trans

import javax.annotation.Nonnull
import javax.swing.JOptionPane

@ArtifactProviderFor(GriffonController)
class ViewProfileController {
    
    @MVCMember @Nonnull
    ViewProfileModel model
    @MVCMember @Nonnull
    ViewProfileView view
    
    @ControllerAction
    void fetch() {
        model.register()
        model.fetch()
    }
    
    @ControllerAction
    void copyFull() {
        CopyPasteSupport.copyToClipboard(model.persona.toBase64())
    }
    
    @ControllerAction
    void addContact() {
        String reason = JOptionPane.showInputDialog(trans("ENTER_REASON_OPTIONAL"))
        model.core.eventBus.publish(new TrustEvent(persona: model.persona, level: TrustLevel.TRUSTED, 
                reason: reason, profileHeader: model.profileHeader, profile: model.profile))
    }
    
    @ControllerAction
    void block() {
        String reason = JOptionPane.showInputDialog(trans("ENTER_REASON_OPTIONAL"))
        model.core.eventBus.publish(new TrustEvent(persona: model.persona, level: TrustLevel.DISTRUSTED, 
                reason: reason, profileHeader: model.profileHeader, profile: model.profile))
    }
    
    @ControllerAction
    void close() {
        view.window.setVisible(false)
        mvcGroup.destroy()
    }
}
