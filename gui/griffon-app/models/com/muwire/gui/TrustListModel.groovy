package com.muwire.gui

import com.muwire.core.trust.RemoteTrustList
import com.muwire.core.trust.TrustService
import com.muwire.gui.profile.TrustPOP
import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class TrustListModel {
    RemoteTrustList trustList
    TrustService trustService

    List<TrustPOP> trusted
    List<TrustPOP> distrusted

    void mvcGroupInit(Map<String,String> args) {
        trusted = trustList.good.collect {new TrustPOP(it)}
        distrusted = trustList.bad.collect {new TrustPOP(it)}
    }
}