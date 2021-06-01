package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import groovy.util.logging.Log

import java.util.logging.Level

import javax.annotation.Nonnull
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import java.awt.Font
import java.awt.SystemTray

import com.muwire.core.Core
import com.muwire.core.MuWireSettings

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonController)
class OptionsController {
    @MVCMember @Nonnull
    OptionsModel model
    @MVCMember @Nonnull
    OptionsView view
    
    Core core
    MuWireSettings settings
    UISettings uiSettings

    @ControllerAction
    void save() {
        String text

        def i2pProps = core.i2pOptions

        int tunnelLength = view.tunnelLengthSlider.value
        i2pProps["inbound.length"] = String.valueOf(tunnelLength)
        i2pProps["outbound.length"] = String.valueOf(tunnelLength)
        
        int tunnelQuantity = view.tunnelQuantitySlider.value
        i2pProps["inbound.quantity"] = String.valueOf(tunnelQuantity)
        i2pProps["outbound.quantity"] = String.valueOf(tunnelQuantity)
        
        if (settings.embeddedRouter) {
            text = view.i2pNTCPPortField.text
            model.i2pNTCPPort = text
            i2pProps["i2np.ntcp.port"] = text

            text = view.i2pUDPPortField.text
            model.i2pUDPPort = text
            i2pProps["i2np.udp.port"] = text
        }


        File i2pSettingsFile = new File(core.home, "i2p.properties")
        i2pSettingsFile.withOutputStream {
            i2pProps.store(it,"")
        }

        text = view.retryField.text
        model.downloadRetryInterval = text
        settings.downloadRetryInterval = Integer.valueOf(text)
        
        text = view.downloadMaxFailuresField.text
        model.downloadMaxFailures = text
        settings.downloadMaxFailures = Integer.valueOf(text)

        if (!settings.disableUpdates) {
            text = view.updateField.text
            model.updateCheckInterval = text
            settings.updateCheckInterval = Integer.valueOf(text)

            boolean autoDownloadUpdate = view.autoDownloadUpdateCheckbox.model.isSelected()
            model.autoDownloadUpdate = autoDownloadUpdate
            settings.autoDownloadUpdate = autoDownloadUpdate
        }
                
        text = view.totalUploadSlotsField.text
        int totalUploadSlots = Integer.valueOf(text)
        model.totalUploadSlots = totalUploadSlots
        settings.totalUploadSlots = totalUploadSlots
        
        text = view.uploadSlotsPerUserField.text
        int uploadSlotsPerUser = Integer.valueOf(text)
        model.uploadSlotsPerUser = uploadSlotsPerUser
        settings.uploadSlotsPerUser = uploadSlotsPerUser

        boolean searchComments = view.searchCommentsCheckbox.model.isSelected()
        model.searchComments = searchComments
        settings.searchComments = searchComments
        
        boolean searchCollections = view.searchCollectionsCheckbox.model.isSelected()
        model.searchCollections = searchCollections
        settings.searchCollections = searchCollections

        boolean shareDownloaded = view.shareDownloadedCheckbox.model.isSelected()
        model.shareDownloadedFiles = shareDownloaded
        settings.shareDownloadedFiles = shareDownloaded
        
        boolean shareHidden = view.shareHiddenCheckbox.model.isSelected()
        model.shareHiddenFiles = shareHidden
        settings.shareHiddenFiles = shareHidden
        
        int hashingCores = Integer.parseInt(view.hashingCoresTextField.text)
        model.hashingCores = hashingCores
        settings.hashingCores = hashingCores

        boolean browseFiles = view.browseFilesCheckbox.model.isSelected()
        model.browseFiles = browseFiles
        settings.browseFiles = browseFiles
        
        boolean allowTracking = view.allowTrackingCheckbox.model.isSelected()
        model.allowTracking = allowTracking
        settings.allowTracking = allowTracking
        
        text = view.speedSmoothSecondsField.text
        model.speedSmoothSeconds = Integer.valueOf(text)
        settings.speedSmoothSeconds = Integer.valueOf(text)
        
        if (!view.isAqua()) {
            String downloadLocation = model.downloadLocation
            settings.downloadLocation = new File(downloadLocation)

            String incompleteLocation = model.incompleteLocation
            settings.incompleteLocation = new File(incompleteLocation)
        }

        if (settings.embeddedRouter) {
            text = view.inBwField.text
            model.inBw = text
            settings.inBw = Integer.valueOf(text)
            text = view.outBwField.text
            model.outBw = text
            settings.outBw = Integer.valueOf(text)
        }
        
        // feed saving
        
        boolean fileFeed = view.fileFeedCheckbox.model.isSelected()
        model.fileFeed = fileFeed
        settings.fileFeed = fileFeed
        
        boolean advertiseFeed = view.advertiseFeedCheckbox.model.isSelected()
        model.advertiseFeed = advertiseFeed
        settings.advertiseFeed = advertiseFeed
        
        boolean autoPublishSharedFiles = view.autoPublishSharedFilesCheckbox.model.isSelected()
        model.autoPublishSharedFiles = autoPublishSharedFiles
        settings.autoPublishSharedFiles = autoPublishSharedFiles
        
        boolean defaultFeedAutoDownload = view.defaultFeedAutoDownloadCheckbox.model.isSelected()
        model.defaultFeedAutoDownload = defaultFeedAutoDownload
        settings.defaultFeedAutoDownload = defaultFeedAutoDownload
        
        boolean defaultFeedSequential = view.defaultFeedSequentialCheckbox.model.isSelected()
        model.defaultFeedSequential = defaultFeedSequential
        settings.defaultFeedSequential = defaultFeedSequential
        
        String defaultFeedItemsToKeep = view.defaultFeedItemsToKeepField.text
        model.defaultFeedItemsToKeep = defaultFeedItemsToKeep
        settings.defaultFeedItemsToKeep = Integer.parseInt(defaultFeedItemsToKeep)
        
        String defaultFeedUpdateInterval = view.defaultFeedUpdateIntervalField.text
        model.defaultFeedUpdateInterval = defaultFeedUpdateInterval
        settings.defaultFeedUpdateInterval = Integer.parseInt(defaultFeedUpdateInterval) * 60000L

        // trust saving

        boolean onlyTrusted = view.allowUntrustedCheckbox.model.isSelected()
        model.onlyTrusted = onlyTrusted
        settings.setAllowUntrusted(!onlyTrusted)

        boolean searchExtraHop = view.searchExtraHopCheckbox.model.isSelected()
        model.searchExtraHop = searchExtraHop
        settings.searchExtraHop = searchExtraHop
        
        boolean trustLists = view.allowTrustListsCheckbox.model.isSelected()
        model.trustLists = trustLists
        settings.allowTrustLists = trustLists

        String trustListInterval = view.trustListIntervalField.text
        model.trustListInterval = trustListInterval
        settings.trustListInterval = Integer.parseInt(trustListInterval)
        
        // chat settings
        
        boolean startChatServer = view.startChatServerCheckbox.model.isSelected()
        model.startChatServer = startChatServer
        settings.startChatServer = startChatServer
        
        String maxChatConnections = view.maxChatConnectionsField.text
        model.maxChatConnections = Integer.parseInt(maxChatConnections)
        settings.maxChatConnections = Integer.parseInt(maxChatConnections)
        
        boolean advertiseChat = view.advertiseChatCheckbox.model.isSelected()
        model.advertiseChat = advertiseChat
        settings.advertiseChat = advertiseChat
        
        int maxChatLines = Integer.parseInt(view.maxChatLinesField.text)
        model.maxChatLines = maxChatLines
        uiSettings.maxChatLines = maxChatLines
        
        if (model.chatWelcomeFile != null)
            settings.chatWelcomeFile = new File(model.chatWelcomeFile)
            
        // messaging settings
        
        boolean allowMessages = view.allowMessagesCheckbox.model.isSelected()
        model.allowMessages = allowMessages
        settings.allowMessages = allowMessages
        
        boolean allowOnlyTrustedMessages = view.allowOnlyTrustedMessagesCheckbox.model.isSelected()
        model.allowOnlyTrustedMessages = allowOnlyTrustedMessages
        settings.allowOnlyTrustedMessages = allowOnlyTrustedMessages
        
        int messageSendInterval = Integer.parseInt(view.messageSendIntervalField.text)
        model.messageSendInterval = messageSendInterval
        settings.messageSendInterval = messageSendInterval
        
        if (view.messageNotificationsCheckbox != null) {
            boolean messageNotifications = view.messageNotificationsCheckbox.model.isSelected()
            model.messageNotifications = messageNotifications
            uiSettings.messageNotifications = messageNotifications
        }
        
        
        core.saveMuSettings()

        // UI Setttings

        if (model.systemLnf)
            text = "system"
        else if (model.darculaLnf)
            text = "com.bulenkov.darcula.DarculaLaf"
        else
            text = "metal"
        uiSettings.lnf = text

        text = view.fontField.text
        model.font = text
        uiSettings.font = text
        
        uiSettings.autoFontSize = model.automaticFontSize
        uiSettings.fontSize = Integer.parseInt(view.fontSizeField.text)
        
        uiSettings.fontStyle = Font.PLAIN
        if (view.fontStyleBoldCheckbox.model.isSelected())
            uiSettings.fontStyle |= Font.BOLD
        if (view.fontStyleItalicCheckbox.model.isSelected())
            uiSettings.fontStyle |= Font.ITALIC

        uiSettings.groupByFile = model.groupByFile
        
        boolean clearCancelledDownloads = view.clearCancelledDownloadsCheckbox.model.isSelected()
        model.clearCancelledDownloads = clearCancelledDownloads
        uiSettings.clearCancelledDownloads = clearCancelledDownloads

        boolean clearFinishedDownloads = view.clearFinishedDownloadsCheckbox.model.isSelected()
        model.clearFinishedDownloads = clearFinishedDownloads
        uiSettings.clearFinishedDownloads = clearFinishedDownloads

        boolean excludeLocalResult = view.excludeLocalResultCheckbox.model.isSelected()
        model.excludeLocalResult = excludeLocalResult
        uiSettings.excludeLocalResult = excludeLocalResult
        
        boolean clearUploads = view.clearUploadsCheckbox.model.isSelected()
        model.clearUploads = clearUploads
        uiSettings.clearUploads = clearUploads
        
        boolean storeSearchHistory = view.storeSearchHistoryCheckbox.model.isSelected()
        model.storeSearchHistory = storeSearchHistory
        uiSettings.storeSearchHistory = storeSearchHistory
        
        if (SystemTray.isSupported()) {
            uiSettings.exitOnClose = model.exitOnClose
            if (model.closeDecisionMade)
                uiSettings.closeWarning = false
        }
            
        saveUISettings()

        cancel()
    }
    
    private void saveUISettings() {
        File uiSettingsFile = new File(core.home, "gui.properties")
        uiSettingsFile.withOutputStream {
            uiSettings.write(it)
        }
    }

    @ControllerAction
    void cancel() {
        view.d.setVisible(false)
        mvcGroup.destroy()
    }

    @ControllerAction
    void downloadLocation() {
        def chooser = new JFileChooser()
        chooser.setFileHidingEnabled(false)
        chooser.setDialogTitle(trans("OPTIONS_SELECT_LOCATION_DOWNLOADED_FILES"))
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        int rv = chooser.showOpenDialog(null)
        if (rv == JFileChooser.APPROVE_OPTION)
            model.downloadLocation = chooser.getSelectedFile().getAbsolutePath()
    }
    
    @ControllerAction
    void incompleteLocation() {
        def chooser = new JFileChooser()
        chooser.setFileHidingEnabled(false)
        chooser.setDialogTitle(trans("OPTIONS_SELECT_LOCATION_INCOMPLETE_FILES"))
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        int rv = chooser.showOpenDialog(null)
        if (rv == JFileChooser.APPROVE_OPTION)
            model.incompleteLocation = chooser.getSelectedFile().getAbsolutePath()
    }
    
    @ControllerAction
    void chooseChatFile() {
        def chooser = new JFileChooser()
        chooser.with { 
            setFileHidingEnabled(false)
            setDialogTitle(trans("OPTIONS_SELECT_CHAT_SERVER_FILE"))
            setFileSelectionMode(JFileChooser.FILES_ONLY)
        }
        int rv = chooser.showOpenDialog(null)
        if (rv == JFileChooser.APPROVE_OPTION)
            model.chatWelcomeFile = chooser.getSelectedFile().getAbsolutePath()
    }

    @ControllerAction
    void systemLnf() {
        model.systemLnf = true
        model.darculaLnf = false
        model.metalLnf = false
    }

    @ControllerAction
    void darculaLnf() {
        model.darculaLnf = true
        model.systemLnf = false
        model.metalLnf = false
    }

    @ControllerAction
    void metalLnf() {
        model.metalLnf = true
        model.darculaLnf = false
        model.systemLnf = false
    }

    @ControllerAction
    void automaticFont() {
        model.automaticFontSize = true
        model.customFontSize = 12
    }
    
    @ControllerAction
    void customFont() {
        model.automaticFontSize = false
    }
    
    @ControllerAction
    void exitOnClose() {
        model.exitOnClose = true
        model.closeDecisionMade = true
    }
    
    @ControllerAction
    void minimizeOnClose() {
        model.exitOnClose = false
        model.closeDecisionMade = true
    }
    
    @ControllerAction
    void groupByFile() {
        model.groupByFile = true
    }
    
    @ControllerAction
    void groupBySender() {
        model.groupByFile = false
    }
    
    @ControllerAction
    void clearHistory() {
        uiSettings.searchHistory.clear()
        saveUISettings()
        JOptionPane.showMessageDialog(null, trans("OPTIONS_SEARCH_HISTORY_CLEARED"))
    }
}