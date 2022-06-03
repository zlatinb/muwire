package com.muwire.gui

import com.muwire.core.trust.RemoteTrustList
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService
import com.muwire.core.trust.TrustService.TrustEntry
import com.muwire.gui.profile.TrustPOP
import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class TrustListModel {
    RemoteTrustList trustList
    TrustService trustService

    List<RemoteTrustPOP> contacts

    void mvcGroupInit(Map<String,String> args) {
        contacts = trustList.good.collect {new RemoteTrustPOP(it, TrustLevel.TRUSTED)}
        contacts.addAll(trustList.bad.collect {new RemoteTrustPOP(it, TrustLevel.DISTRUSTED)})
    }
    
    private static class RemoteTrustPOP extends TrustPOP {
        final TrustLevel level
        RemoteTrustPOP(TrustEntry entry, TrustLevel level) {
            super(entry)
            this.level = level
        }
    }
}