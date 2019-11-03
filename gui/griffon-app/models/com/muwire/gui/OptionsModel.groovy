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
    @Observable boolean shareDownloadedFiles
    @Observable boolean shareHiddenFiles
    @Observable String downloadLocation
    @Observable String incompleteLocation
    @Observable boolean searchComments
    @Observable boolean browseFiles
    @Observable int speedSmoothSeconds
    @Observable int totalUploadSlots
    @Observable int uploadSlotsPerUser
    @Observable boolean storeSearchHistory

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
    @Observable boolean automaticFontSize
    @Observable int customFontSize
    @Observable boolean clearCancelledDownloads
    @Observable boolean clearFinishedDownloads
    @Observable boolean excludeLocalResult
    @Observable boolean showSearchHashes
    @Observable boolean clearUploads
    @Observable boolean exitOnClose
    @Observable boolean closeDecisionMade 

    // bw options
    @Observable String inBw
    @Observable String outBw

    // trust options
    @Observable boolean onlyTrusted
    @Observable boolean searchExtraHop
    @Observable boolean trustLists
    @Observable String trustListInterval


    void mvcGroupInit(Map<String, String> args) {
        MuWireSettings settings = application.context.get("muwire-settings")
        downloadRetryInterval = settings.downloadRetryInterval
        updateCheckInterval = settings.updateCheckInterval
        autoDownloadUpdate = settings.autoDownloadUpdate
        shareDownloadedFiles = settings.shareDownloadedFiles
        shareHiddenFiles = settings.shareHiddenFiles
        downloadLocation = settings.downloadLocation.getAbsolutePath()
        incompleteLocation = settings.incompleteLocation.getAbsolutePath()
        searchComments = settings.searchComments
        browseFiles = settings.browseFiles
        speedSmoothSeconds = settings.speedSmoothSeconds
        totalUploadSlots = settings.totalUploadSlots
        uploadSlotsPerUser = settings.uploadSlotsPerUser

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
        automaticFontSize = uiSettings.autoFontSize
        customFontSize = uiSettings.fontSize
        clearCancelledDownloads = uiSettings.clearCancelledDownloads
        clearFinishedDownloads = uiSettings.clearFinishedDownloads
        excludeLocalResult = uiSettings.excludeLocalResult
        showSearchHashes = uiSettings.showSearchHashes
        clearUploads = uiSettings.clearUploads
        exitOnClose = uiSettings.exitOnClose
        storeSearchHistory = uiSettings.storeSearchHistory

        if (core.router != null) {
            inBw = String.valueOf(settings.inBw)
            outBw = String.valueOf(settings.outBw)
        }

        onlyTrusted = !settings.allowUntrusted()
        searchExtraHop = settings.searchExtraHop
        trustLists = settings.allowTrustLists
        trustListInterval = String.valueOf(settings.trustListInterval)
    }
}