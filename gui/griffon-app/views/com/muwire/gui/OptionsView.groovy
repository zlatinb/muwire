package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import groovy.swing.factory.TitledBorderFactory

import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingConstants
import javax.swing.border.TitledBorder

import com.muwire.core.Core

import java.awt.BorderLayout
import java.awt.GridBagConstraints
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
    def shareHiddenCheckbox
    def searchCommentsCheckbox
    def browseFilesCheckbox
    def speedSmoothSecondsField

    def inboundLengthField
    def inboundQuantityField
    def outboundLengthField
    def outboundQuantityField
    def i2pUDPPortField
    def i2pNTCPPortField

    def lnfField
    def monitorCheckbox
    def fontField
    def fontSizeField
    def clearCancelledDownloadsCheckbox
    def clearFinishedDownloadsCheckbox
    def excludeLocalResultCheckbox
    def showSearchHashesCheckbox

    def inBwField
    def outBwField

    def allowUntrustedCheckbox
    def searchExtraHopCheckbox
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
            panel (border : titledBorder(title : "Search Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx: 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL)) {
                gridBagLayout()
                label(text : "Search in comments", constraints:gbc(gridx: 0, gridy:0, anchor : GridBagConstraints.LINE_START, 
                    fill : GridBagConstraints.HORIZONTAL, weightx: 100))
                searchCommentsCheckbox = checkBox(selected : bind {model.searchComments}, constraints : gbc(gridx:1, gridy:0,
                anchor : GridBagConstraints.LINE_END, fill : GridBagConstraints.HORIZONTAL, weightx: 0))
                label(text : "Allow browsing", constraints : gbc(gridx : 0, gridy : 1, anchor : GridBagConstraints.LINE_START, 
                    fill : GridBagConstraints.HORIZONTAL, weightx: 100))
                browseFilesCheckbox = checkBox(selected : bind {model.browseFiles}, constraints : gbc(gridx : 1, gridy : 1,
                anchor : GridBagConstraints.LINE_END, fill : GridBagConstraints.HORIZONTAL, weightx: 0))
            }
            
            panel (border : titledBorder(title : "Download Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL))) {
                gridBagLayout()
                label(text : "Retry failed downloads every", constraints : gbc(gridx: 0, gridy: 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                retryField = textField(text : bind { model.downloadRetryInterval }, columns : 2, 
                    constraints : gbc(gridx: 1, gridy: 0, anchor : GridBagConstraints.LINE_END, weightx: 0))
                label(text : "seconds", constraints : gbc(gridx : 2, gridy: 0, weightx : 0, anchor : GridBagConstraints.LINE_START))
                
                label(text : "Save downloaded files to:", constraints: gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START))
                label(text : bind {model.downloadLocation}, constraints: gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_START))
                button(text : "Choose", constraints : gbc(gridx : 2, gridy:1), downloadLocationAction)
                
                label(text : "Store incomplete files in:", constraints: gbc(gridx:0, gridy:2, anchor : GridBagConstraints.LINE_START))
                label(text : bind {model.incompleteLocation}, constraints: gbc(gridx:1, gridy:2, anchor : GridBagConstraints.LINE_START))
                button(text : "Choose", constraints : gbc(gridx : 2, gridy:2), incompleteLocationAction)
            }
            
            panel (border : titledBorder(title : "Sharing Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx : 0, gridy : 2, fill : GridBagConstraints.HORIZONTAL))) {
                gridBagLayout()
                label(text : "Share downloaded files", constraints : gbc(gridx : 0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                shareDownloadedCheckbox = checkBox(selected : bind {model.shareDownloadedFiles}, constraints : gbc(gridx :1, gridy:0, weightx : 0))
                
                label(text : "Share hidden files", constraints : gbc(gridx : 0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                shareHiddenCheckbox = checkBox(selected : bind {model.shareHiddenFiles}, constraints : gbc(gridx :1, gridy:1, weightx : 0))
            }
            
            panel (border : titledBorder(title : "Update Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx : 0, gridy : 3, fill : GridBagConstraints.HORIZONTAL))) {
                gridBagLayout()
                label(text : "Check for updates every", constraints : gbc(gridx : 0, gridy: 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                updateField = textField(text : bind {model.updateCheckInterval }, columns : 2, constraints : gbc(gridx : 1, gridy: 0, weightx: 0))
                label(text : "hours", constraints : gbc(gridx: 2, gridy : 0, weightx: 0, anchor : GridBagConstraints.LINE_START))

                label(text : "Download updates automatically", constraints: gbc(gridx :0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                autoDownloadUpdateCheckbox = checkBox(selected : bind {model.autoDownloadUpdate}, 
                    constraints : gbc(gridx:2, gridy : 1, anchor : GridBagConstraints.LINE_END, fill : GridBagConstraints.HORIZONTAL))

            }
        }
        i = builder.panel {
            gridBagLayout()
            label(text : "Changing these settings requires a restart", constraints : gbc(gridx : 0, gridy : 0, gridwidth: 2, anchor:GridBagConstraints.PAGE_START))
            label(text : "Inbound Length", constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START))
            inboundLengthField = textField(text : bind {model.inboundLength}, columns : 2, constraints : gbc(gridx:1, gridy:1))
            label(text : "Inbound Quantity", constraints : gbc(gridx:0, gridy:2, anchor : GridBagConstraints.LINE_START))
            inboundQuantityField = textField(text : bind {model.inboundQuantity}, columns : 2, constraints : gbc(gridx:1, gridy:2))
            label(text : "Outbound Length", constraints : gbc(gridx:0, gridy:3, anchor : GridBagConstraints.LINE_START))
            outboundLengthField = textField(text : bind {model.outboundLength}, columns : 2, constraints : gbc(gridx:1, gridy:3))
            label(text : "Outbound Quantity", constraints : gbc(gridx:0, gridy:4, anchor : GridBagConstraints.LINE_START))
            outboundQuantityField = textField(text : bind {model.outboundQuantity}, columns : 2, constraints : gbc(gridx:1, gridy:4))

            Core core = application.context.get("core")
            if (core.router != null) {
                label(text : "TCP Port", constraints : gbc(gridx :0, gridy: 5, anchor : GridBagConstraints.LINE_START))
                i2pNTCPPortField = textField(text : bind {model.i2pNTCPPort}, columns : 4, constraints : gbc(gridx:1, gridy:5))
                label(text : "UDP Port", constraints : gbc(gridx :0, gridy: 6, anchor : GridBagConstraints.LINE_START))
                i2pUDPPortField = textField(text : bind {model.i2pUDPPort}, columns : 4, constraints : gbc(gridx:1, gridy:6))
            }
            panel(constraints : gbc(gridx: 0, gridy: 7, weighty: 100))

        }
        u = builder.panel {
            gridBagLayout()
            panel (border : titledBorder(title : "Theme Settings (changes require restart)", border : etchedBorder(), titlePosition: TitledBorder.TOP,
            constraints : gbc(gridx : 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL))) {
                gridBagLayout()
                label(text : "Look And Feel", constraints : gbc(gridx: 0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                lnfField = textField(text : bind {model.lnf}, columns : 4, constraints : gbc(gridx : 1, gridy : 0, anchor : GridBagConstraints.LINE_START))
                label(text : "Font", constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                fontField = textField(text : bind {model.font}, columns : 4, constraints : gbc(gridx : 1, gridy:1, anchor : GridBagConstraints.LINE_START))

                label(text : "Font Size", constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx : 100))
                buttonGroup(id: "fontSizeGroup")
                radioButton(text: "Automatic", selected : bind {model.automaticFontSize}, buttonGroup : fontSizeGroup,
                constraints : gbc(gridx : 1, gridy: 2, anchor : GridBagConstraints.LINE_START), automaticFontAction)
                radioButton(text: "Custom", selected : bind {!model.automaticFontSize}, buttonGroup : fontSizeGroup,
                constraints : gbc(gridx : 1, gridy: 3, anchor : GridBagConstraints.LINE_START), customFontAction)
                fontSizeField = textField(text : bind {model.customFontSize}, enabled : bind {!model.automaticFontSize}, constraints : gbc(gridx : 2, gridy : 3))

            }
            panel (border : titledBorder(title : "Other Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP,
            constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL))) {
                gridBagLayout()
                label(text : "Automatically Clear Cancelled Downloads", constraints: gbc(gridx: 0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                clearCancelledDownloadsCheckbox = checkBox(selected : bind {model.clearCancelledDownloads},
                constraints : gbc(gridx : 1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : "Automatically Clear Finished Downloads", constraints: gbc(gridx: 0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                clearFinishedDownloadsCheckbox = checkBox(selected : bind {model.clearFinishedDownloads},
                constraints : gbc(gridx : 1, gridy:1, anchor : GridBagConstraints.LINE_END))
                label(text : "Smooth Download Speed Over (seconds)", constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx: 100))
                speedSmoothSecondsField = textField(text : bind {model.speedSmoothSeconds},
                constraints : gbc(gridx:1, gridy: 2, anchor : GridBagConstraints.LINE_START))
                label(text : "Exclude Local Files From Results", constraints: gbc(gridx:0, gridy:3, anchor : GridBagConstraints.LINE_START, weightx: 100))
                excludeLocalResultCheckbox = checkBox(selected : bind {model.excludeLocalResult},
                constraints : gbc(gridx: 1, gridy : 3, anchor : GridBagConstraints.LINE_END))
            }
            panel (constraints : gbc(gridx: 0, gridy: 2, weighty: 100))
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
            label(text : "Allow only trusted connections", constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START))
            allowUntrustedCheckbox = checkBox(selected : bind {model.onlyTrusted}, constraints : gbc(gridx: 1, gridy : 0))
            label(text : "Search extra hop", constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START))
            searchExtraHopCheckbox = checkBox(selected : bind {model.searchExtraHop}, constraints : gbc(gridx: 1, gridy : 1))
            label(text : "Allow others to view my trust list", constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START))
            allowTrustListsCheckbox = checkBox(selected : bind {model.trustLists}, constraints : gbc(gridx: 1, gridy : 2))
            label(text : "Update trust lists every ", constraints : gbc(gridx:0, gridy:3, anchor : GridBagConstraints.LINE_START))
            trustListIntervalField = textField(text : bind {model.trustListInterval}, constraints:gbc(gridx:1, gridy:3))
            label(text : "hours", constraints : gbc(gridx: 2, gridy:3))
            
            panel(constraints : gbc(gridx: 0, gridy : 4, weighty: 100))
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