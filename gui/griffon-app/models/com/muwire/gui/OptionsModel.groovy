package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.MuWireSettings

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class OptionsModel {
    @Observable String downloadRetryInterval 
    @Observable String updateCheckInterval
    @Observable boolean autoDownloadUpdate
    @Observable boolean onlyTrusted
    @Observable boolean shareDownloadedFiles
    @Observable String downloadLocation
    
    // i2p options
    @Observable String inboundLength
    @Observable String inboundQuantity
    @Observable String outboundLength
    @Observable String outboundQuantity
    @Observable String i2pUDPPort
    @Observable String i2pNTCPPort
    
    // gui options
    @Observable boolean showMonitor
    @Observable String lnf
    @Observable String font
    @Observable boolean clearCancelledDownloads
    @Observable boolean clearFinishedDownloads
    @Observable boolean excludeLocalResult
    @Observable boolean showSearchHashes
    
    // bw options
    @Observable String inBw
    @Observable String outBw
    
    void mvcGroupInit(Map<String, String> args) {
        MuWireSettings settings = application.context.get("muwire-settings")
        downloadRetryInterval = settings.downloadRetryInterval
        updateCheckInterval = settings.updateCheckInterval
        autoDownloadUpdate = settings.autoDownloadUpdate
        onlyTrusted = !settings.allowUntrusted()
        shareDownloadedFiles = settings.shareDownloadedFiles
        downloadLocation = settings.downloadLocation.getAbsolutePath()
        
        Core core = application.context.get("core")
        inboundLength = core.i2pOptions["inbound.length"]
        inboundQuantity = core.i2pOptions["inbound.quantity"]
        outboundLength = core.i2pOptions["outbound.length"]
        outboundQuantity = core.i2pOptions["outbound.quantity"]
        i2pUDPPort = core.i2pOptions["i2np.udp.port"]
        i2pNTCPPort = core.i2pOptions["i2np.ntcp.port"]
        
        UISettings uiSettings = application.context.get("ui-settings")
        showMonitor = uiSettings.showMonitor
        lnf = uiSettings.lnf
        font = uiSettings.font
        clearCancelledDownloads = uiSettings.clearCancelledDownloads
        clearFinishedDownloads = uiSettings.clearFinishedDownloads
        excludeLocalResult = uiSettings.excludeLocalResult
        showSearchHashes = uiSettings.showSearchHashes
        
        if (core.router != null) {
            inBw = String.valueOf(settings.inBw)
            outBw = String.valueOf(settings.outBw)
        }
    }
}