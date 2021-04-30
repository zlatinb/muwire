package com.muwire.gui

import griffon.core.artifact.GriffonController
import static com.muwire.gui.Translator.trans
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull
import javax.swing.JOptionPane

import com.muwire.core.Persona
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

@ArtifactProviderFor(GriffonController)
class AddContactController {
    @MVCMember @Nonnull
    AddContactModel model
    @MVCMember @Nonnull
    AddContactView view

    @ControllerAction
    void add() {
        String text = view.idArea.getText().trim()
        Persona p
        try {
            p = new Persona(new ByteArrayInputStream(Base64.decode(text)))
            TrustLevel tl = model.trusted ? TrustLevel.TRUSTED : TrustLevel.DISTRUSTED
            TrustEvent e = new TrustEvent(persona : p, level : tl, reason : view.reasonArea.getText())
            model.core.eventBus.publish(e)
            cancel()
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, trans("ADD_CONTACT_INVALID_ID_BODY"), 
                trans("ADD_CONTACT_INVALID_ID_TITLE"), JOptionPane.WARNING_MESSAGE)
            return
        }
    }
    
    @ControllerAction
    void cancel() {
        view.dialog.setVisible(false)
        mvcGroup.destroy()
    }
}