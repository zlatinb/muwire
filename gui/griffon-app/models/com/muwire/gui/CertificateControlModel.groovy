package com.muwire.gui

import com.muwire.core.Core

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class CertificateControlModel {
    def users = []
    def certificates = []
    
    Core core
    
    void mvcGroupInit(Map<String,String> args) {
        users.addAll(core.certificateManager.byIssuer.keySet())
    }
}