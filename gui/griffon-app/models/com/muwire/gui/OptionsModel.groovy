package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.MuWireSettings

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

import java.awt.Font
import java.awt.GraphicsEnvironment

@ArtifactProviderFor(GriffonModel)
class OptionsModel {
    @Observable String downloadRetryInterval
    @Observable String downloadMaxFailures
    @Observable String updateCheckInterval
    @Observable boolean autoDownloadUpdate
    @Observable boolean shareDownloadedFiles
    @Observable boolean shareHiddenFiles
    @Observable int hashingCores
    @Observable boolean throttleLoadingFiles
    @Observable String ignoredFileTypes
    @Observable String downloadLocation
    @Observable String incompleteLocation
    @Observable boolean searchComments
    @Observable boolean searchCollections
    @Observable boolean searchPaths
    @Observable boolean browseFiles
    @Observable boolean showPaths
    @Observable boolean allowTracking
    @Observable boolean regexQueries
    @Observable int speedSmoothSeconds
    @Observable int totalUploadSlots
    @Observable int uploadSlotsPerUser
    @Observable boolean storeSearchHistory

    // i2p options
    @Observable int tunnelLength
    @Observable int tunnelQuantity
    @Observable String i2pUDPPort
    @Observable String i2pNTCPPort
    @Observable boolean useUPNP
    @Observable String i2cpHost
    @Observable String i2cpPort
    @Observable boolean embeddedRouter

    // gui options
    @Observable boolean showMonitor
    @Observable String lnfClassName
    @Observable String font
    String [] availableFonts
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
    @Observable boolean showUnsharedPaths
    @Observable boolean messageNotifications

    // bw options
    @Observable String inBw
    @Observable String outBw
    @Observable int sharePercentage

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
    @Observable boolean searchExtraHopCheckboxEnabled
    @Observable boolean trustLists
    @Observable String trustListInterval

    // chat options
    @Observable boolean startChatServer
    @Observable int maxChatConnections
    @Observable boolean advertiseChat
    @Observable int maxChatLines
    @Observable boolean chatNotifyMentions
    @Observable String chatWelcomeFile
    @Observable String defaultChatRoom
    @Observable boolean joinDefaultChatRoom
    
    // messaging options
    @Observable boolean allowMessages
    @Observable boolean allowOnlyTrustedMessages
    @Observable int messageSendInterval
    
    // persona rendering options
    @Observable boolean personaRendererAvatars
    @Observable boolean personaRendererIds
    
    boolean disableUpdates

    void mvcGroupInit(Map<String, String> args) {
        MuWireSettings settings = application.context.get("muwire-settings")
        downloadRetryInterval = settings.downloadRetryInterval
        downloadMaxFailures = settings.downloadMaxFailures
        updateCheckInterval = settings.updateCheckInterval
        autoDownloadUpdate = settings.autoDownloadUpdate
        shareDownloadedFiles = settings.shareDownloadedFiles
        shareHiddenFiles = settings.shareHiddenFiles
        hashingCores = settings.hashingCores
        throttleLoadingFiles = settings.throttleLoadingFiles
        ignoredFileTypes = settings.ignoredFileTypes.join(",")
        downloadLocation = settings.downloadLocation.getAbsolutePath()
        incompleteLocation = settings.incompleteLocation.getAbsolutePath()
        searchComments = settings.searchComments
        searchCollections = settings.searchCollections
        searchPaths = settings.searchPaths
        browseFiles = settings.browseFiles
        showPaths = settings.showPaths
        allowTracking = settings.allowTracking
        regexQueries = settings.regexQueries
        speedSmoothSeconds = settings.speedSmoothSeconds
        totalUploadSlots = settings.totalUploadSlots
        uploadSlotsPerUser = settings.uploadSlotsPerUser

        Core core = application.context.get("core")
        tunnelLength = Math.max(Integer.parseInt(core.i2pOptions["inbound.length"]), Integer.parseInt(core.i2pOptions['outbound.length']))
        tunnelQuantity = Math.max(Integer.parseInt(core.i2pOptions["inbound.quantity"]), Integer.parseInt(core.i2pOptions['outbound.quantity']))
        i2pUDPPort = core.i2pOptions["i2np.udp.port"]
        i2pNTCPPort = core.i2pOptions["i2np.ntcp.port"]
        useUPNP = Boolean.parseBoolean(core.i2pOptions.getProperty("i2np.upnp.enable","true"))
        i2cpHost = core.i2pOptions["i2cp.tcp.host"]
        i2cpPort = core.i2pOptions["i2cp.tcp.port"]
        embeddedRouter = core.muOptions.embeddedRouter

        UISettings uiSettings = application.context.get("ui-settings")
        showMonitor = uiSettings.showMonitor
        lnfClassName = uiSettings.lnf
        font = uiSettings.font
        availableFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
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
        messageNotifications = uiSettings.messageNotifications
        showUnsharedPaths = uiSettings.showUnsharedPaths
        personaRendererAvatars = uiSettings.personaRendererAvatars
        personaRendererIds = uiSettings.personaRendererIds

        if (System.getProperty("embeddedRouter") == "true") {
            inBw = String.valueOf(settings.inBw)
            outBw = String.valueOf(settings.outBw)
            sharePercentage = settings.sharePercentage
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
        searchExtraHopCheckboxEnabled = onlyTrusted
        trustLists = settings.allowTrustLists
        trustListInterval = String.valueOf(settings.trustListInterval)
        
        startChatServer = settings.startChatServer
        maxChatConnections = settings.maxChatConnections
        advertiseChat = settings.advertiseChat
        maxChatLines = uiSettings.maxChatLines
        chatNotifyMentions = uiSettings.chatNotifyMentions
        chatWelcomeFile = settings.chatWelcomeFile?.getAbsolutePath()
        defaultChatRoom = settings.defaultChatRoom
        joinDefaultChatRoom = settings.joinDefaultChatRoom
        
        allowMessages = settings.allowMessages
        allowOnlyTrustedMessages = settings.allowOnlyTrustedMessages
        messageSendInterval = settings.messageSendInterval
        
        disableUpdates = settings.disableUpdates
    }
}
