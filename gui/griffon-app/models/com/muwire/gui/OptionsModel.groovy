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
    @Observable boolean onlyTrusted
    @Observable boolean shareDownloadedFiles
    
    // i2p options
    @Observable String inboundLength
    @Observable String inboundQuantity
    @Observable String outboundLength
    @Observable String outboundQuantity
    
    void mvcGroupInit(Map<String, String> args) {
        MuWireSettings settings = application.context.get("muwire-settings")
        downloadRetryInterval = settings.downloadRetryInterval
        updateCheckInterval = settings.updateCheckInterval
        onlyTrusted = !settings.allowUntrusted()
        shareDownloadedFiles = settings.shareDownloadedFiles
        
        Core core = application.context.get("core")
        inboundLength = core.i2pOptions["inbound.length"]
        inboundQuantity = core.i2pOptions["inbound.quantity"]
        outboundLength = core.i2pOptions["outbound.length"]
        outboundQuantity = core.i2pOptions["outbound.quantity"]
    }
}