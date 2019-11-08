package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.filecert.Certificate

@ArtifactProviderFor(GriffonController)
class SharedFileController {
    @MVCMember @Nonnull
    SharedFileView view
    @MVCMember @Nonnull
    SharedFileModel model
    
    @ControllerAction
    void showComment() {
        Certificate cert = view.getSelectedCertificate()
        if (cert == null || cert.comment == null)
            return
        
        def params = [:]
        params['text'] = cert.comment.name
        mvcGroup.createMVCGroup('show-comment',params)
    }
}