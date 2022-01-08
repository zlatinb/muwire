package com.muwire.gui.resultdetails

import com.muwire.core.filecert.Certificate
import com.muwire.core.filecert.UIImportCertificateEvent
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.swing.JOptionPane

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonController)
class CertificateTabController {
    @MVCMember @Nonnull
    CertificateTabModel model
    @MVCMember @Nonnull
    CertificateTabView view
    
    @ControllerAction
    void fetchCertificates() {
        view.switchToTable()
        model.register()
    }
    
    @ControllerAction
    void importCerts() {
        List<Certificate> selected = view.selectedCertificates()
        selected.each {
            model.core.eventBus.publish(new UIImportCertificateEvent(certificate : it))
        }
        JOptionPane.showMessageDialog(null, trans("CERTIFICATES_IMPORTED"))
    }
    
    @ControllerAction
    void viewComment() {
        List<Certificate> selected = view.selectedCertificates()
        if (selected.size() != 1)
            return
        String comment = selected[0].comment.name
        def params = [:]
        params['text'] = comment
        params['name'] = trans("CERTIFICATE_COMMENT")
        mvcGroup.createMVCGroup("show-comment", params).destroy()
    }
}
