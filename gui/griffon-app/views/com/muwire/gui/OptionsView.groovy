package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import groovy.swing.factory.TitledBorderFactory

import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSlider
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
    def feed
    def trust
    def chat

    def retryField
    def updateField
    def autoDownloadUpdateCheckbox
    def shareDownloadedCheckbox
    def shareHiddenCheckbox
    def searchCommentsCheckbox
    def browseFilesCheckbox
    def allowTrackingCheckbox
    def speedSmoothSecondsField
    def totalUploadSlotsField
    def uploadSlotsPerUserField

    def tunnelLengthSlider
    def tunnelQuantitySlider
    def i2pUDPPortField
    def i2pNTCPPortField

    def lnfField
    def monitorCheckbox
    def fontField
    def fontSizeField
    def fontStyleBoldCheckbox
    def fontStyleItalicCheckbox
    def clearCancelledDownloadsCheckbox
    def clearFinishedDownloadsCheckbox
    def excludeLocalResultCheckbox
    def showSearchHashesCheckbox
    def clearUploadsCheckbox
    def storeSearchHistoryCheckbox

    def inBwField
    def outBwField
    
    def fileFeedCheckbox
    def advertiseFeedCheckbox
    def autoPublishSharedFilesCheckbox
    def defaultFeedAutoDownloadCheckbox
    def defaultFeedItemsToKeepField
    def defaultFeedSequentialCheckbox
    def defaultFeedUpdateIntervalField

    def allowUntrustedCheckbox
    def searchExtraHopCheckbox
    def allowTrustListsCheckbox
    def trustListIntervalField

    def startChatServerCheckbox
    def maxChatConnectionsField
    def advertiseChatCheckbox
    def maxChatLinesField
    
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
                label(text : "Allow tracking", constraints : gbc(gridx: 0, gridy: 2, anchor: GridBagConstraints.LINE_START,
                    fill : GridBagConstraints.HORIZONTAL, weightx: 100))
                allowTrackingCheckbox = checkBox(selected : bind {model.allowTracking}, constraints : gbc(gridx: 1, gridy : 2,
                    anchor : GridBagConstraints.LINE_END, fill : GridBagConstraints.HORIZONTAL, weightx : 0))
            }
            
            panel (border : titledBorder(title : "Download Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL))) {
                gridBagLayout()
                label(text : "Retry failed downloads every (seconds)", constraints : gbc(gridx: 0, gridy: 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                retryField = textField(text : bind { model.downloadRetryInterval }, columns : 2, 
                    constraints : gbc(gridx: 2, gridy: 0, anchor : GridBagConstraints.LINE_END, weightx: 0))
                
                label(text : "Save downloaded files to:", constraints: gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START))
                label(text : bind {model.downloadLocation}, constraints: gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_START))
                button(text : "Choose", constraints : gbc(gridx : 2, gridy:1), downloadLocationAction)
                
                label(text : "Store incomplete files in:", constraints: gbc(gridx:0, gridy:2, anchor : GridBagConstraints.LINE_START))
                label(text : bind {model.incompleteLocation}, constraints: gbc(gridx:1, gridy:2, anchor : GridBagConstraints.LINE_START))
                button(text : "Choose", constraints : gbc(gridx : 2, gridy:2), incompleteLocationAction)
            }
            
            panel (border : titledBorder(title : "Upload Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx : 0, gridy:2, fill : GridBagConstraints.HORIZONTAL))) {
                gridBagLayout()
                label(text : "Total upload slots (-1 means unlimited)", constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                totalUploadSlotsField = textField(text : bind {model.totalUploadSlots}, columns: 2,
                    constraints : gbc(gridx : 1, gridy: 0, anchor : GridBagConstraints.LINE_END))
                label(text : "Upload slots per user (-1 means unlimited)", constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                uploadSlotsPerUserField = textField(text : bind {model.uploadSlotsPerUser}, columns: 2,
                    constraints : gbc(gridx : 1, gridy: 1, anchor : GridBagConstraints.LINE_END))
            }
            
            panel (border : titledBorder(title : "Sharing Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx : 0, gridy : 3, fill : GridBagConstraints.HORIZONTAL))) {
                gridBagLayout()
                label(text : "Share downloaded files", constraints : gbc(gridx : 0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                shareDownloadedCheckbox = checkBox(selected : bind {model.shareDownloadedFiles}, constraints : gbc(gridx :1, gridy:0, weightx : 0))
                
                label(text : "Share hidden files", constraints : gbc(gridx : 0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                shareHiddenCheckbox = checkBox(selected : bind {model.shareHiddenFiles}, constraints : gbc(gridx :1, gridy:1, weightx : 0))
            }
            
            panel (border : titledBorder(title : "Update Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx : 0, gridy : 4, fill : GridBagConstraints.HORIZONTAL))) {
                gridBagLayout()
                label(text : "Check for updates every (hours)", constraints : gbc(gridx : 0, gridy: 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                updateField = textField(text : bind {model.updateCheckInterval }, columns : 2, constraints : gbc(gridx : 1, gridy: 0, weightx: 0))

                label(text : "Download updates automatically", constraints: gbc(gridx :0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                autoDownloadUpdateCheckbox = checkBox(selected : bind {model.autoDownloadUpdate}, 
                    constraints : gbc(gridx:1, gridy : 1, anchor : GridBagConstraints.LINE_END))

            }
        }
        i = builder.panel {
            gridBagLayout()
            label(text : "Changing any I2P settings requires a restart", constraints : gbc(gridx:0, gridy : 0))
            panel (border : titledBorder(title : "Tunnel Settings", border : etchedBorder(), titlePosition: TitledBorder.TOP,
            constraints : gbc(gridx: 0, gridy: 1, fill : GridBagConstraints.HORIZONTAL, weightx : 100))) {
                gridLayout(rows:4, cols:1)
                
                label(text : "Speed vs Anonymity")
                def lengthTable = new Hashtable()
                lengthTable.put(1, new JLabel("Max Speed"))
                lengthTable.put(3, new JLabel("Max Anonymity"))
                tunnelLengthSlider = slider(minimum : 1, maximum : 3, value : bind {model.tunnelLength}, 
                    majorTickSpacing : 1, snapToTicks: true, paintTicks: true, labelTable : lengthTable,
                    paintLabels : true)
                
                
                label(text: "Reliability vs Resource Usage")
                def quantityTable = new Hashtable()
                quantityTable.put(1, new JLabel("Min Resources"))
                quantityTable.put(6, new JLabel("Max Reliability"))
                tunnelQuantitySlider = slider(minimum : 1, maximum : 6, value : bind {model.tunnelQuantity}, 
                    majorTickSpacing : 1, snapToTicks : true, paintTicks: true, labelTable : quantityTable,
                    paintLabels : true)
            }

            Core core = application.context.get("core")
            if (core.router != null) {
                panel(border : titledBorder(title : "Port Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx: 0, gridy : 2, fill : GridBagConstraints.HORIZONTAL, weightx: 100))) {
                    gridBagLayout()
                    label(text : "TCP port", constraints : gbc(gridx :0, gridy: 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                    i2pNTCPPortField = textField(text : bind {model.i2pNTCPPort}, columns : 4, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                    label(text : "UDP port", constraints : gbc(gridx :0, gridy: 1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                    i2pUDPPortField = textField(text : bind {model.i2pUDPPort}, columns : 4, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
                }
            }
            panel(constraints : gbc(gridx: 0, gridy: 3, weighty: 100))

        }
        u = builder.panel {
            gridBagLayout()
            panel (border : titledBorder(title : "Theme Settings (changes require restart)", border : etchedBorder(), titlePosition: TitledBorder.TOP,
            constraints : gbc(gridx : 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx : 100))) {
                gridBagLayout()
                label(text : "Look And Feel", constraints : gbc(gridx: 0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                lnfField = textField(text : bind {model.lnf}, columns : 4, constraints : gbc(gridx : 3, gridy : 0, anchor : GridBagConstraints.LINE_START))
                label(text : "Font", constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                fontField = textField(text : bind {model.font}, columns : 4, constraints : gbc(gridx : 3, gridy:1, anchor : GridBagConstraints.LINE_START))

                label(text : "Font size", constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx : 100))
                buttonGroup(id: "fontSizeGroup")
                radioButton(text: "Automatic", selected : bind {model.automaticFontSize}, buttonGroup : fontSizeGroup,
                constraints : gbc(gridx : 1, gridy: 2, anchor : GridBagConstraints.LINE_START), automaticFontAction)
                radioButton(text: "Custom", selected : bind {!model.automaticFontSize}, buttonGroup : fontSizeGroup,
                constraints : gbc(gridx : 2, gridy: 2, anchor : GridBagConstraints.LINE_START), customFontAction)
                fontSizeField = textField(text : bind {model.customFontSize}, enabled : bind {!model.automaticFontSize}, 
                    constraints : gbc(gridx : 3, gridy : 2, anchor : GridBagConstraints.LINE_END))
                
                label(text : "Font style", constraints: gbc(gridx: 0, gridy: 3, anchor : GridBagConstraints.LINE_START, weightx: 100))
                panel(constraints : gbc(gridx: 2, gridy: 3, gridwidth: 2, anchor:GridBagConstraints.LINE_END)) {
                    fontStyleBoldCheckbox = checkBox(selected : bind {model.fontStyleBold})
                    label(text: "Bold")
                    fontStyleItalicCheckbox = checkBox(selected : bind {model.fontStyleItalic})
                    label(text: "Italic")
                }

            }
            panel (border : titledBorder(title : "Search Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL, weightx : 100)) {
                gridBagLayout()
                label(text : "By default, group search results by", constraints : gbc(gridx :0, gridy: 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                panel(constraints : gbc(gridx : 1, gridy : 0, anchor : GridBagConstraints.LINE_END)) {
                    buttonGroup(id : "groupBy")
                    radioButton(text : "Sender", selected : bind {!model.groupByFile}, buttonGroup : groupBy, groupBySenderAction)
                    radioButton(text : "File", selected : bind {model.groupByFile}, buttonGroup : groupBy, groupByFileAction)
                }
                label(text : "Remember search history", constraints: gbc(gridx: 0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                storeSearchHistoryCheckbox = checkBox(selected : bind {model.storeSearchHistory},
                constraints : gbc(gridx : 1, gridy:1, anchor : GridBagConstraints.LINE_END))
                button(text : "Clear history", constraints : gbc(gridx : 1, gridy : 2, anchor : GridBagConstraints.LINE_END), clearHistoryAction)
                
            }
            panel (border : titledBorder(title : "Other Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 2, fill : GridBagConstraints.HORIZONTAL, weightx : 100)) {
                gridBagLayout()
                label(text : "Automatically clear cancelled downloads", constraints: gbc(gridx: 0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                clearCancelledDownloadsCheckbox = checkBox(selected : bind {model.clearCancelledDownloads},
                constraints : gbc(gridx : 1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : "Automatically clear finished downloads", constraints: gbc(gridx: 0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                clearFinishedDownloadsCheckbox = checkBox(selected : bind {model.clearFinishedDownloads},
                constraints : gbc(gridx : 1, gridy:1, anchor : GridBagConstraints.LINE_END))
                label(text : "Smooth download speed over (seconds)", constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx: 100))
                speedSmoothSecondsField = textField(text : bind {model.speedSmoothSeconds},
                constraints : gbc(gridx:1, gridy: 2, anchor : GridBagConstraints.LINE_END))
                label(text : "Exclude local files from results", constraints: gbc(gridx:0, gridy:3, anchor : GridBagConstraints.LINE_START, weightx: 100))
                excludeLocalResultCheckbox = checkBox(selected : bind {model.excludeLocalResult},
                constraints : gbc(gridx: 1, gridy : 3, anchor : GridBagConstraints.LINE_END))
                label(text : "Automatically clear finished uploads", constraints:gbc(gridx:0, gridy:4, anchor: GridBagConstraints.LINE_START, weightx : 100))
                clearUploadsCheckbox = checkBox(selected : bind {model.clearUploads},
                constraints : gbc(gridx:1, gridy: 4, anchor:GridBagConstraints.LINE_END))
                label(text : "When closing MuWire", constraints : gbc(gridx: 0, gridy : 5, anchor : GridBagConstraints.LINE_START, weightx: 100))
                panel (constraints : gbc(gridx:1, gridy: 5, anchor : GridBagConstraints.LINE_END)) {
                    buttonGroup(id : "closeBehaviorGroup")
                    radioButton(text : "Minimize to tray", selected : bind {!model.exitOnClose}, buttonGroup: closeBehaviorGroup, minimizeOnCloseAction)
                    radioButton(text : "Exit", selected : bind {model.exitOnClose}, buttonGroup : closeBehaviorGroup, exitOnCloseAction)
                }
            }
            panel (constraints : gbc(gridx: 0, gridy: 3, weighty: 100))
        }
        bandwidth = builder.panel {
            gridBagLayout()
            panel( border : titledBorder(title : "Changing bandwidth settings requires a restart", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx : 100)) {
                gridBagLayout()
                label(text : "Inbound bandwidth (KB)", constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                inBwField = textField(text : bind {model.inBw}, columns : 3, constraints : gbc(gridx : 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                label(text : "Outbound bandwidth (KB)", constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                outBwField = textField(text : bind {model.outBw}, columns : 3, constraints : gbc(gridx : 1, gridy : 1, anchor : GridBagConstraints.LINE_END))
            }
            panel(constraints : gbc(gridx: 0, gridy: 1, weighty: 100))
        }
        feed = builder.panel {
            gridBagLayout()
            panel (border : titledBorder(title : "General Feed Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP),
                constraints : gbc(gridx : 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "Enable file feed", constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                fileFeedCheckbox = checkBox(selected : bind {model.fileFeed}, constraints : gbc(gridx: 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                label(text : "Advertise feed in search results", constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                advertiseFeedCheckbox = checkBox(selected : bind {model.advertiseFeed}, constraints : gbc(gridx: 1, gridy : 1, anchor : GridBagConstraints.LINE_END))
                label(text : "Automatically publish shared files", constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx: 100))
                autoPublishSharedFilesCheckbox = checkBox(selected : bind {model.autoPublishSharedFiles}, constraints : gbc(gridx: 1, gridy : 2, anchor : GridBagConstraints.LINE_END))
            }
            panel (border : titledBorder(title : "Default Settings For New Feeds", border : etchedBorder(), titlePosition : TitledBorder.TOP),
                constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "Automatically download files from feed", constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                defaultFeedAutoDownloadCheckbox = checkBox(selected : bind {model.defaultFeedAutoDownload}, constraints : gbc(gridx: 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                label(text : "Download files from feed sequentially", constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                defaultFeedSequentialCheckbox = checkBox(selected : bind {model.defaultFeedSequential}, constraints : gbc(gridx: 1, gridy : 1, anchor : GridBagConstraints.LINE_END))
                label(text : "Feed items to store on disk (-1 means unlimited)", constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx: 100))
                defaultFeedItemsToKeepField = textField(text : bind {model.defaultFeedItemsToKeep}, constraints:gbc(gridx :1, gridy:2, anchor : GridBagConstraints.LINE_END))
                label(text : "Feed refresh frequency in minutes", constraints : gbc(gridx: 0, gridy : 3, anchor : GridBagConstraints.LINE_START, weightx: 100))
                defaultFeedUpdateIntervalField = textField(text : bind {model.defaultFeedUpdateInterval}, constraints:gbc(gridx :1, gridy:3, anchor : GridBagConstraints.LINE_END))
            }
            panel(constraints : gbc(gridx: 0, gridy : 2, weighty: 100))
        }
        trust = builder.panel {
            gridBagLayout()
            panel (border : titledBorder(title : "Trust Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "Allow only trusted connections", constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                allowUntrustedCheckbox = checkBox(selected : bind {model.onlyTrusted}, constraints : gbc(gridx: 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                label(text : "Search extra hop", constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                searchExtraHopCheckbox = checkBox(selected : bind {model.searchExtraHop}, constraints : gbc(gridx: 1, gridy : 1, anchor : GridBagConstraints.LINE_END))
                label(text : "Allow others to view my trust list", constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx : 100))
                allowTrustListsCheckbox = checkBox(selected : bind {model.trustLists}, constraints : gbc(gridx: 1, gridy : 2, anchor : GridBagConstraints.LINE_END))
                label(text : "Update trust lists every (hours)", constraints : gbc(gridx:0, gridy:3, anchor : GridBagConstraints.LINE_START, weightx : 100))
                trustListIntervalField = textField(text : bind {model.trustListInterval}, constraints:gbc(gridx:1, gridy:3, anchor : GridBagConstraints.LINE_END))
            }
            panel(constraints : gbc(gridx: 0, gridy : 1, weighty: 100))
        }
        
        chat = builder.panel {
            gridBagLayout()
            panel (border : titledBorder(title : "Chat Settings", border : etchedBorder(), titlePosition : TitledBorder.TOP),
                constraints : gbc(gridx : 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : "Start chat server on startup", constraints : gbc(gridx: 0, gridy: 0, anchor: GridBagConstraints.LINE_START, weightx: 100))
                startChatServerCheckbox = checkBox(selected : bind{model.startChatServer}, constraints : gbc(gridx:2, gridy:0, anchor:GridBagConstraints.LINE_END))
                label(text : "Maximum chat connections (-1 means unlimited)", constraints : gbc(gridx: 0, gridy:1, anchor:GridBagConstraints.LINE_START, weightx:100))
                maxChatConnectionsField = textField(text : bind {model.maxChatConnections}, constraints : gbc(gridx: 2, gridy : 1, anchor:GridBagConstraints.LINE_END))
                label(text : "Advertise chat ability in search results", constraints : gbc(gridx: 0, gridy:2, anchor:GridBagConstraints.LINE_START, weightx:100))
                advertiseChatCheckbox = checkBox(selected : bind{model.advertiseChat}, constraints : gbc(gridx:2, gridy:2, anchor:GridBagConstraints.LINE_END))
                label(text : "Maximum lines of scrollback (-1 means unlimited)", constraints : gbc(gridx:0, gridy:3, anchor : GridBagConstraints.LINE_START, weightx: 100))
                maxChatLinesField = textField(text : bind{model.maxChatLines}, constraints : gbc(gridx:2, gridy: 3, anchor: GridBagConstraints.LINE_END))
                label(text : "Welcome message file", constraints : gbc(gridx : 0, gridy : 4, anchor : GridBagConstraints.LINE_START, weightx: 100))
                label(text : bind {model.chatWelcomeFile}, constraints : gbc(gridx : 1, gridy : 4))
                button(text : "Choose", constraints : gbc(gridx : 2, gridy : 4, anchor : GridBagConstraints.LINE_END), chooseChatFileAction)
            }
            panel(constraints : gbc(gridx: 0, gridy : 1, weighty: 100))
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
        tabbedPane.addTab("Feed", feed)
        tabbedPane.addTab("Trust", trust)
        tabbedPane.addTab("Chat", chat)

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