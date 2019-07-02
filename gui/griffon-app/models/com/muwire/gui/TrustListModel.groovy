package com.muwire.gui

import com.muwire.core.trust.RemoteTrustList

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class TrustListModel {
    RemoteTrustList trustList
}