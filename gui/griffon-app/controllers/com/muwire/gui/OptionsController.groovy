package com.muwire.gui

import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import groovy.util.logging.Log

import javax.inject.Inject
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.plaf.FontUIResource
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
    @Inject @Nonnull
    GriffonApplication application
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
            
            boolean useUPNP = view.useUPNPCheckbox.model.isSelected()
            model.useUPNP = useUPNP
            i2pProps["i2np.upnp.enable"] = String.valueOf(useUPNP)
            i2pProps["i2np.upnp.ipv6.enable"] = String.valueOf(useUPNP)
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
        
        boolean searchPaths = view.searchPathsCheckbox.model.isSelected()
        model.searchPaths = searchPaths
        settings.searchPaths = searchPaths

        boolean shareDownloaded = view.shareDownloadedCheckbox.model.isSelected()
        model.shareDownloadedFiles = shareDownloaded
        settings.shareDownloadedFiles = shareDownloaded
        
        boolean shareHidden = view.shareHiddenCheckbox.model.isSelected()
        model.shareHiddenFiles = shareHidden
        settings.shareHiddenFiles = shareHidden
        
        int hashingCores = Integer.parseInt(view.hashingCoresTextField.text)
        model.hashingCores = hashingCores
        settings.hashingCores = hashingCores

        text = view.ignoredFileTypesTextField.text
        model.ignoredFileTypes = text
        settings.ignoredFileTypes.clear()
        text.split(",").each {settings.ignoredFileTypes.add(it)}
      
        boolean throttleLoadingFiles = view.throttleLoadingFilesCheckbox.model.isSelected()
        model.throttleLoadingFiles = throttleLoadingFiles
        settings.throttleLoadingFiles = throttleLoadingFiles
        
        boolean browseFiles = view.browseFilesCheckbox.model.isSelected()
        model.browseFiles = browseFiles
        settings.browseFiles = browseFiles
        
        boolean showPaths = view.showPathsCheckbox.model.isSelected()
        model.showPaths = showPaths
        settings.showPaths = showPaths
        
        boolean allowTracking = view.allowTrackingCheckbox.model.isSelected()
        model.allowTracking = allowTracking
        settings.allowTracking = allowTracking
        
        boolean regexQueries = view.regexQueriesCheckbox.model.isSelected()
        model.regexQueries = regexQueries
        settings.regexQueries = regexQueries
        
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
            int sharePercentage = view.sharePercentageSlider.value
            model.sharePercentage = sharePercentage
            settings.sharePercentage = sharePercentage
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
        
        boolean joinDefaultChatRoom = view.joinDefaultChatRoomCheckbox.model.isSelected()
        model.joinDefaultChatRoom = joinDefaultChatRoom
        settings.joinDefaultChatRoom = joinDefaultChatRoom
        
        String defaultChatRoom = view.defaultChatRoomField.text
        model.defaultChatRoom = defaultChatRoom
        settings.defaultChatRoom = defaultChatRoom
        
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

        boolean applyLNF = false
        text = view.lnfComboBox.getSelectedItem()
        uiSettings.lnf = LNFs.getLNFClassName(text)
        applyLNF |= (model.lnfClassName != uiSettings.lnf)

        text = view.fontComboBox.getSelectedItem()
        uiSettings.font = text
        applyLNF |= (model.font != text)
        
        applyLNF |= (uiSettings.autoFontSize != model.automaticFontSize)
        uiSettings.autoFontSize = model.automaticFontSize
        
        int newFontSize = Integer.parseInt(view.fontSizeField.text)
        applyLNF |= (newFontSize != uiSettings.fontSize)
        uiSettings.fontSize = newFontSize
        
        int newFontStyle = Font.PLAIN
        if (view.fontStyleBoldCheckbox.model.isSelected())
            newFontStyle |= Font.BOLD
        if (view.fontStyleItalicCheckbox.model.isSelected())
            newFontStyle |= Font.ITALIC
        applyLNF |= (newFontStyle != uiSettings.fontStyle)
        uiSettings.fontStyle = newFontStyle

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
        
        boolean showUnsharedPaths = view.showUnsharedPathsCheckbox.model.isSelected()
        model.showUnsharedPaths = showUnsharedPaths
        uiSettings.showUnsharedPaths = showUnsharedPaths
        
        boolean storeSearchHistory = view.storeSearchHistoryCheckbox.model.isSelected()
        model.storeSearchHistory = storeSearchHistory
        uiSettings.storeSearchHistory = storeSearchHistory
        
        if (SystemTray.isSupported()) {
            uiSettings.exitOnClose = model.exitOnClose
            if (model.closeDecisionMade)
                uiSettings.closeWarning = false
        }
            
        saveUISettings()

        if (applyLNF)
            updateLNF()
        cancel()
    }
    
    private void saveUISettings() {
        File uiSettingsFile = new File(core.home, "gui.properties")
        uiSettingsFile.withOutputStream {
            uiSettings.write(it)
        }
        uiSettings.notifyListeners()
    }

    @ControllerAction
    void cancel() {
        view.d.setVisible(false)
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
    
    private void updateLNF() {
        
        UIManager.setLookAndFeel(uiSettings.lnf)
        def lnf = UIManager.getLookAndFeel()
        
        FontUIResource font = new FontUIResource(uiSettings.font, uiSettings.fontStyle, uiSettings.fontSize)
        def keys = lnf.getDefaults().keys()
        while(keys.hasMoreElements()) {
            def key = keys.nextElement()
            def value = lnf.getDefaults().get(key)
            if (value instanceof FontUIResource) {
                lnf.getDefaults().put(key, font)
                UIManager.put(key, font)
            }
        }
        
        def mainWindow = application.getWindowManager().findWindow("main-frame")
        SwingUtilities.updateComponentTreeUI(mainWindow)
        
    }
}