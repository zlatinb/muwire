package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.MuWireSettings

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

import java.awt.Font

@ArtifactProviderFor(GriffonModel)
class OptionsModel {
    @Observable String downloadRetryInterval
    @Observable String downloadMaxFailures
    @Observable String updateCheckInterval
    @Observable boolean autoDownloadUpdate
    @Observable boolean shareDownloadedFiles
    @Observable boolean shareHiddenFiles
    @Observable String downloadLocation
    @Observable String incompleteLocation
    @Observable boolean searchComments
    @Observable boolean searchCollections
    @Observable boolean browseFiles
    @Observable boolean allowTracking
    @Observable int speedSmoothSeconds
    @Observable int totalUploadSlots
    @Observable int uploadSlotsPerUser
    @Observable boolean storeSearchHistory

    // i2p options
    @Observable int tunnelLength
    @Observable int tunnelQuantity
    @Observable String i2pUDPPort
    @Observable String i2pNTCPPort

    // gui options
    @Observable boolean showMonitor
    @Observable String lnf
    @Observable String font
    @Observable boolean automaticFontSize
    @Observable int customFontSize
    @Observable boolean fontStyleBold
    @Observable boolean fontStyleItalic
    @Observable boolean clearCancelledDownloads
    @Observable boolean clearFinishedDownloads
    @Observable boolean excludeLocalResult
    @Observable boolean showSearchHashes
    @Observable boolean clearUploads
    @Observable boolean groupByFile
    @Observable boolean exitOnClose
    @Observable boolean closeDecisionMade 

    // bw options
    @Observable String inBw
    @Observable String outBw

    // feed options
    @Observable boolean fileFeed
    @Observable boolean advertiseFeed
    @Observable boolean autoPublishSharedFiles
    @Observable boolean defaultFeedAutoDownload
    @Observable String defaultFeedItemsToKeep
    @Observable boolean defaultFeedSequential
    @Observable String defaultFeedUpdateInterval
    
    // trust options
    @Observable boolean onlyTrusted
    @Observable boolean searchExtraHop
    @Observable boolean trustLists
    @Observable String trustListInterval

    // chat options
    @Observable boolean startChatServer
    @Observable int maxChatConnections
    @Observable boolean advertiseChat
    @Observable int maxChatLines
    @Observable String chatWelcomeFile
    
    // messaging options
    @Observable boolean allowMessages
    @Observable boolean allowOnlyTrustedMessages
    
    boolean disableUpdates

    void mvcGroupInit(Map<String, String> args) {
        MuWireSettings settings = application.context.get("muwire-settings")
        downloadRetryInterval = settings.downloadRetryInterval
        downloadMaxFailures = settings.downloadMaxFailures
        updateCheckInterval = settings.updateCheckInterval
        autoDownloadUpdate = settings.autoDownloadUpdate
        shareDownloadedFiles = settings.shareDownloadedFiles
        shareHiddenFiles = settings.shareHiddenFiles
        downloadLocation = settings.downloadLocation.getAbsolutePath()
        incompleteLocation = settings.incompleteLocation.getAbsolutePath()
        searchComments = settings.searchComments
        searchCollections = settings.searchCollections
        browseFiles = settings.browseFiles
        allowTracking = settings.allowTracking
        speedSmoothSeconds = settings.speedSmoothSeconds
        totalUploadSlots = settings.totalUploadSlots
        uploadSlotsPerUser = settings.uploadSlotsPerUser

        Core core = application.context.get("core")
        tunnelLength = Math.max(Integer.parseInt(core.i2pOptions["inbound.length"]), Integer.parseInt(core.i2pOptions['outbound.length']))
        tunnelQuantity = Math.max(Integer.parseInt(core.i2pOptions["inbound.quantity"]), Integer.parseInt(core.i2pOptions['outbound.quantity']))
        i2pUDPPort = core.i2pOptions["i2np.udp.port"]
        i2pNTCPPort = core.i2pOptions["i2np.ntcp.port"]

        UISettings uiSettings = application.context.get("ui-settings")
        showMonitor = uiSettings.showMonitor
        lnf = uiSettings.lnf
        font = uiSettings.font
        automaticFontSize = uiSettings.autoFontSize
        customFontSize = uiSettings.fontSize
        fontStyleBold = (uiSettings.fontStyle & Font.BOLD) == Font.BOLD
        fontStyleItalic = (uiSettings.fontStyle & Font.ITALIC) == Font.ITALIC
        clearCancelledDownloads = uiSettings.clearCancelledDownloads
        clearFinishedDownloads = uiSettings.clearFinishedDownloads
        excludeLocalResult = uiSettings.excludeLocalResult
        showSearchHashes = uiSettings.showSearchHashes
        clearUploads = uiSettings.clearUploads
        exitOnClose = uiSettings.exitOnClose
        storeSearchHistory = uiSettings.storeSearchHistory
        groupByFile = uiSettings.groupByFile

        if (core.router != null) {
            inBw = String.valueOf(settings.inBw)
            outBw = String.valueOf(settings.outBw)
        }
        
        fileFeed = settings.fileFeed
        advertiseFeed = settings.advertiseFeed
        autoPublishSharedFiles = settings.autoPublishSharedFiles
        defaultFeedAutoDownload = settings.defaultFeedAutoDownload
        defaultFeedItemsToKeep = String.valueOf(settings.defaultFeedItemsToKeep)
        defaultFeedSequential = settings.defaultFeedSequential
        defaultFeedUpdateInterval = String.valueOf(Math.max(1L, (long)(settings.defaultFeedUpdateInterval / 60000L)))

        onlyTrusted = !settings.allowUntrusted()
        searchExtraHop = settings.searchExtraHop
        trustLists = settings.allowTrustLists
        trustListInterval = String.valueOf(settings.trustListInterval)
        
        startChatServer = settings.startChatServer
        maxChatConnections = settings.maxChatConnections
        advertiseChat = settings.advertiseChat
        maxChatLines = uiSettings.maxChatLines
        chatWelcomeFile = settings.chatWelcomeFile?.getAbsolutePath()
        
        allowMessages = settings.allowMessages
        allowOnlyTrustedMessages = settings.allowOnlyTrustedMessages
        
        disableUpdates = settings.disableUpdates
    }
}