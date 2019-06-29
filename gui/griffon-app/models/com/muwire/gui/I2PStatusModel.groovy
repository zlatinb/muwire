package com.muwire.gui

import javax.annotation.Nonnull

import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class I2PStatusModel {
    @MVCMember @Nonnull
    I2PStatusController controller
    
    @Observable int ntcpConnections
    @Observable int ssuConnections
    @Observable String networkStatus
    @Observable int participatingTunnels
    @Observable int activePeers
    @Observable int receiveBps
    @Observable int sendBps
    @Observable int participatingBW
    
    void mvcGroupInit(Map<String,String> args) {
        controller.refresh()
    }
}