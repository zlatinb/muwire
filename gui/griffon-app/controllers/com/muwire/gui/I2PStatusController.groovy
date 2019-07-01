package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.router.Router

import javax.annotation.Nonnull

import com.muwire.core.Core

@ArtifactProviderFor(GriffonController)
class I2PStatusController {
    @MVCMember @Nonnull
    I2PStatusModel model
    @MVCMember @Nonnull
    I2PStatusView view

    @ControllerAction
    void refresh() {
        Core core = application.context.get("core")
        Router router = core.router
        model.networkStatus = router._context.commSystem().status.toStatusString()
        model.floodfill = router._context.netDb().floodfillEnabled()
        model.ntcpConnections = router._context.commSystem().getTransports()["NTCP"].countPeers()
        model.ssuConnections = router._context.commSystem().getTransports()["SSU"].countPeers()
        model.participatingTunnels = router._context.tunnelManager().getParticipatingCount()
        model.activePeers = router._context.profileOrganizer().countActivePeers()
        model.receiveBps = router._context.bandwidthLimiter().getReceiveBps15s()
        model.sendBps = router._context.bandwidthLimiter().getSendBps15s()
        model.participatingBW = router._context.bandwidthLimiter().getCurrentParticipatingBandwidth()
        
    }
    
    @ControllerAction
    void close() {
        view.dialog.setVisible(false)
        mvcGroup.destroy()
    }
}