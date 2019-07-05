package com.muwire.gui

import com.muwire.core.trust.RemoteTrustList
import com.muwire.core.trust.TrustService

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class TrustListModel {
    RemoteTrustList trustList
    TrustService trustService

    def trusted
    def distrusted

    void mvcGroupInit(Map<String,String> args) {
        trusted = new ArrayList<>(trustList.good)
        distrusted = new ArrayList<>(trustList.bad)
    }
}