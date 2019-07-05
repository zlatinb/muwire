package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import groovy.util.logging.Log

import java.util.logging.Level

import javax.annotation.Nonnull
import javax.swing.JFileChooser

import com.muwire.core.Core
import com.muwire.core.MuWireSettings

@ArtifactProviderFor(GriffonController)
class OptionsController {
    @MVCMember @Nonnull
    OptionsModel model
    @MVCMember @Nonnull
    OptionsView view

    @ControllerAction
    void save() {
        String text
        Core core = application.context.get("core")
        MuWireSettings settings = application.context.get("muwire-settings")

        def i2pProps = core.i2pOptions

        text = view.inboundLengthField.text
        model.inboundLength = text
        i2pProps["inbound.length"] = text

        text = view.inboundQuantityField.text
        model.inboundQuantity = text
        i2pProps["inbound.quantity"] = text

        text = view.outboundQuantityField.text
        model.outboundQuantity = text
        i2pProps["outbound.quantity"] = text

        text = view.outboundLengthField.text
        model.outboundLength = text
        i2pProps["outbound.length"] = text

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

        text = view.updateField.text
        model.updateCheckInterval = text
        settings.updateCheckInterval = Integer.valueOf(text)

        boolean autoDownloadUpdate = view.autoDownloadUpdateCheckbox.model.isSelected()
        model.autoDownloadUpdate = autoDownloadUpdate
        settings.autoDownloadUpdate = autoDownloadUpdate


        boolean shareDownloaded = view.shareDownloadedCheckbox.model.isSelected()
        model.shareDownloadedFiles = shareDownloaded
        settings.shareDownloadedFiles = shareDownloaded

        String downloadLocation = model.downloadLocation
        settings.downloadLocation = new File(downloadLocation)

        if (settings.embeddedRouter) {
            text = view.inBwField.text
            model.inBw = text
            settings.inBw = Integer.valueOf(text)
            text = view.outBwField.text
            model.outBw = text
            settings.outBw = Integer.valueOf(text)
        }


        boolean onlyTrusted = view.allowUntrustedCheckbox.model.isSelected()
        model.onlyTrusted = onlyTrusted
        settings.setAllowUntrusted(!onlyTrusted)

        boolean trustLists = view.allowTrustListsCheckbox.model.isSelected()
        model.trustLists = trustLists
        settings.allowTrustLists = trustLists

        String trustListInterval = view.trustListIntervalField.text
        model.trustListInterval = trustListInterval
        settings.trustListInterval = Integer.parseInt(trustListInterval)

        File settingsFile = new File(core.home, "MuWire.properties")
        settingsFile.withOutputStream {
            settings.write(it)
        }

        // UI Setttings

        UISettings uiSettings = application.context.get("ui-settings")
        text = view.lnfField.text
        model.lnf = text
        uiSettings.lnf = text

        text = view.fontField.text
        model.font = text
        uiSettings.font = text

//        boolean showMonitor = view.monitorCheckbox.model.isSelected()
//        model.showMonitor = showMonitor
//        uiSettings.showMonitor = showMonitor

        boolean clearCancelledDownloads = view.clearCancelledDownloadsCheckbox.model.isSelected()
        model.clearCancelledDownloads = clearCancelledDownloads
        uiSettings.clearCancelledDownloads = clearCancelledDownloads

        boolean clearFinishedDownloads = view.clearFinishedDownloadsCheckbox.model.isSelected()
        model.clearFinishedDownloads = clearFinishedDownloads
        uiSettings.clearFinishedDownloads = clearFinishedDownloads

        boolean excludeLocalResult = view.excludeLocalResultCheckbox.model.isSelected()
        model.excludeLocalResult = excludeLocalResult
        uiSettings.excludeLocalResult = excludeLocalResult

//        boolean showSearchHashes = view.showSearchHashesCheckbox.model.isSelected()
//        model.showSearchHashes = showSearchHashes
//        uiSettings.showSearchHashes = showSearchHashes

        File uiSettingsFile = new File(core.home, "gui.properties")
        uiSettingsFile.withOutputStream {
            uiSettings.write(it)
        }

        cancel()
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
        chooser.setDialogTitle("Select location for downloaded files")
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        int rv = chooser.showOpenDialog(null)
        if (rv == JFileChooser.APPROVE_OPTION)
            model.downloadLocation = chooser.getSelectedFile().getAbsolutePath()
    }
}