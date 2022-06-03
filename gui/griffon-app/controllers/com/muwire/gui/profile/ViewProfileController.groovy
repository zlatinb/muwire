package com.muwire.gui.profile

import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel
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
    }
    
    @ControllerAction
    void addContact() {
        String reason = JOptionPane.showInputDialog(trans("ENTER_REASON_OPTIONAL"))
        model.core.eventBus.publish(new TrustEvent(persona: model.persona, level: TrustLevel.TRUSTED, 
                reason: reason, profileHeader: model.profile?.getHeader()))
    }
    
    @ControllerAction
    void block() {
        String reason = JOptionPane.showInputDialog(trans("ENTER_REASON_OPTIONAL"))
        model.core.eventBus.publish(new TrustEvent(persona: model.persona, level: TrustLevel.DISTRUSTED, 
                reason: reason, profileHeader: model.profile?.getHeader()))
    }
    
    @ControllerAction
    void close() {
        view.window.setVisible(false)
        mvcGroup.destroy()
    }
}
