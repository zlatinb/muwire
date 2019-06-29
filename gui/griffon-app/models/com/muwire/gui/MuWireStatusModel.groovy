package com.muwire.gui

import javax.annotation.Nonnull

import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class MuWireStatusModel {
    
    @MVCMember @Nonnull
    MuWireStatusController controller
    
    @Observable int incomingConnections
    @Observable int outgoingConnections
    @Observable int knownHosts
    @Observable int sharedFiles
    @Observable int downloads
    
    void mvcGroupInit(Map<String,String> args) {
        controller.refresh()
    }
}