package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingConstants

import com.muwire.core.Core

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class OptionsView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    OptionsModel model

    def d
    def p
    def i
    def u
    def bandwidth
    def trust

    def retryField
    def updateField
    def autoDownloadUpdateCheckbox
    def shareDownloadedCheckbox

    def inboundLengthField
    def inboundQuantityField
    def outboundLengthField
    def outboundQuantityField
    def i2pUDPPortField
    def i2pNTCPPortField

    def lnfField
    def monitorCheckbox
    def fontField
    def clearCancelledDownloadsCheckbox
    def clearFinishedDownloadsCheckbox
    def excludeLocalResultCheckbox
    def showSearchHashesCheckbox

    def inBwField
    def outBwField

    def allowUntrustedCheckbox
    def allowTrustListsCheckbox
    def trustListIntervalField

    def buttonsPanel

    def mainFrame

    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        d = new JDialog(mainFrame, "Options", true)
        d.setResizable(false)
        p = builder.panel {
            gridBagLayout()
            label(text : "Retry failed downloads every", constraints : gbc(gridx: 0, gridy: 0))
            retryField = textField(text : bind { model.downloadRetryInterval }, columns : 2, constraints : gbc(gridx: 1, gridy: 0))
            label(text : "minutes", constraints : gbc(gridx : 2, gridy: 0))

            label(text : "Check for updates every", constraints : gbc(gridx : 0, gridy: 1))
            updateField = textField(text : bind {model.updateCheckInterval }, columns : 2, constraints : gbc(gridx : 1, gridy: 1))
            label(text : "hours", constraints : gbc(gridx: 2, gridy : 1))

            label(text : "Download updates automatically", constraints: gbc(gridx :0, gridy : 2))
            autoDownloadUpdateCheckbox = checkBox(selected : bind {model.autoDownloadUpdate}, constraints : gbc(gridx:1, gridy : 2))

            label(text : "Share downloaded files", constraints : gbc(gridx : 0, gridy:3))
            shareDownloadedCheckbox = checkBox(selected : bind {model.shareDownloadedFiles}, constraints : gbc(gridx :1, gridy:3))

            label(text : "Save downloaded files to:", constraints: gbc(gridx:0, gridy:4))
            button(text : "Choose", constraints : gbc(gridx : 1, gridy:4), downloadLocationAction)
            label(text : bind {model.downloadLocation}, constraints: gbc(gridx:0, gridy:5, gridwidth:2))

        }
        i = builder.panel {
            gridBagLayout()
            label(text : "Changing these settings requires a restart", constraints : gbc(gridx : 0, gridy : 0, gridwidth: 2))
            label(text : "Inbound Length", constraints : gbc(gridx:0, gridy:1))
            inboundLengthField = textField(text : bind {model.inboundLength}, columns : 2, constraints : gbc(gridx:1, gridy:1))
            label(text : "Inbound Quantity", constraints : gbc(gridx:0, gridy:2))
            inboundQuantityField = textField(text : bind {model.inboundQuantity}, columns : 2, constraints : gbc(gridx:1, gridy:2))
            label(text : "Outbound Length", constraints : gbc(gridx:0, gridy:3))
            outboundLengthField = textField(text : bind {model.outboundLength}, columns : 2, constraints : gbc(gridx:1, gridy:3))
            label(text : "Outbound Quantity", constraints : gbc(gridx:0, gridy:4))
            outboundQuantityField = textField(text : bind {model.outboundQuantity}, columns : 2, constraints : gbc(gridx:1, gridy:4))

            Core core = application.context.get("core")
            if (core.router != null) {
                label(text : "TCP Port", constraints : gbc(gridx :0, gridy: 5))
                i2pNTCPPortField = textField(text : bind {model.i2pNTCPPort}, columns : 4, constraints : gbc(gridx:1, gridy:5))
                label(text : "UDP Port", constraints : gbc(gridx :0, gridy: 6))
                i2pUDPPortField = textField(text : bind {model.i2pUDPPort}, columns : 4, constraints : gbc(gridx:1, gridy:6))
            }

        }
        u = builder.panel {
            gridBagLayout()
            label(text : "Changing these settings requires a restart", constraints : gbc(gridx : 0, gridy : 0, gridwidth: 2))
            label(text : "Look And Feel", constraints : gbc(gridx: 0, gridy:1))
            lnfField = textField(text : bind {model.lnf}, columns : 4, constraints : gbc(gridx : 1, gridy : 1))
            label(text : "Font", constraints : gbc(gridx: 0, gridy : 2))
            fontField = textField(text : bind {model.font}, columns : 4, constraints : gbc(gridx : 1, gridy:2))
//            label(text : "Show Monitor", constraints : gbc(gridx :0, gridy: 3))
//            monitorCheckbox = checkBox(selected : bind {model.showMonitor}, constraints : gbc(gridx : 1, gridy: 3))
            label(text : "Clear Cancelled Downloads", constraints: gbc(gridx: 0, gridy:4))
            clearCancelledDownloadsCheckbox = checkBox(selected : bind {model.clearCancelledDownloads}, constraints : gbc(gridx : 1, gridy:4))
            label(text : "Clear Finished Downloads", constraints: gbc(gridx: 0, gridy:5))
            clearFinishedDownloadsCheckbox = checkBox(selected : bind {model.clearFinishedDownloads}, constraints : gbc(gridx : 1, gridy:5))
            label(text : "Exclude Local Files From Results", constraints: gbc(gridx:0, gridy:6))
            excludeLocalResultCheckbox = checkBox(selected : bind {model.excludeLocalResult}, constraints : gbc(gridx: 1, gridy : 6))
//            label(text : "Show Hash Searches In Monitor", constraints: gbc(gridx:0, gridy:7))
//            showSearchHashesCheckbox = checkBox(selected : bind {model.showSearchHashes}, constraints : gbc(gridx: 1, gridy: 7))
        }
        bandwidth = builder.panel {
            gridBagLayout()
            label(text : "Changing these settings requires a restart", constraints : gbc(gridx : 0, gridy : 0, gridwidth: 2))
            label(text : "Inbound bandwidth (KB)", constraints : gbc(gridx: 0, gridy : 1))
            inBwField = textField(text : bind {model.inBw}, columns : 3, constraints : gbc(gridx : 1, gridy : 1))
            label(text : "Outbound bandwidth (KB)", constraints : gbc(gridx: 0, gridy : 2))
            outBwField = textField(text : bind {model.outBw}, columns : 3, constraints : gbc(gridx : 1, gridy : 2))
        }
        trust = builder.panel {
            gridBagLayout()
            label(text : "Allow only trusted connections", constraints : gbc(gridx: 0, gridy : 0))
            allowUntrustedCheckbox = checkBox(selected : bind {model.onlyTrusted}, constraints : gbc(gridx: 1, gridy : 0))
            label(text : "Allow others to view my trust list", constraints : gbc(gridx: 0, gridy : 1))
            allowTrustListsCheckbox = checkBox(selected : bind {model.trustLists}, constraints : gbc(gridx: 1, gridy : 1))
            label(text : "Update trust lists every ", constraints : gbc(gridx:0, gridy:2))
            trustListIntervalField = textField(text : bind {model.trustListInterval}, constraints:gbc(gridx:1, gridy:2))
            label(text : "hours", constraints : gbc(gridx: 2, gridy:2))
        }


        buttonsPanel = builder.panel {
            gridBagLayout()
            button(text : "Save", constraints : gbc(gridx : 1, gridy: 2), saveAction)
            button(text : "Cancel", constraints : gbc(gridx : 2, gridy: 2), cancelAction)
        }
    }

    void mvcGroupInit(Map<String,String> args) {
        def tabbedPane = new JTabbedPane()
        tabbedPane.addTab("MuWire", p)
        tabbedPane.addTab("I2P", i)
        tabbedPane.addTab("GUI", u)
        Core core = application.context.get("core")
        if (core.router != null) {
            tabbedPane.addTab("Bandwidth", bandwidth)
        }
        tabbedPane.addTab("Trust", trust)

        JPanel panel = new JPanel()
        panel.setLayout(new BorderLayout())
        panel.add(tabbedPane, BorderLayout.CENTER)
        panel.add(buttonsPanel, BorderLayout.SOUTH)

        d.getContentPane().add(panel)
        d.pack()
        d.setLocationRelativeTo(mainFrame)
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        d.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        d.show()
    }
}