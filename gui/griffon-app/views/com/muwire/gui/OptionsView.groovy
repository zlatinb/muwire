package com.muwire.gui

import griffon.core.artifact.GriffonView
import net.i2p.util.SystemVersion

import static com.muwire.gui.Translator.trans
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
import java.awt.SystemTray
import java.awt.Taskbar
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
    def downloadMaxFailuresField
    def updateField
    def autoDownloadUpdateCheckbox
    def shareDownloadedCheckbox
    def shareHiddenCheckbox
    def hashingCoresTextField
    def ignoredFileTypesTextField
    def searchCommentsCheckbox
    def searchCollectionsCheckbox
    def browseFilesCheckbox
    def allowTrackingCheckbox
    def speedSmoothSecondsField
    def totalUploadSlotsField
    def uploadSlotsPerUserField

    def tunnelLengthSlider
    def tunnelQuantitySlider
    def i2pUDPPortField
    def i2pNTCPPortField

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
    def messageNotificationsCheckbox

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
    
    def allowMessagesCheckbox
    def allowOnlyTrustedMessagesCheckbox
    def messageSendIntervalField
    
    def buttonsPanel

    def mainFrame

    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        d = new JDialog(mainFrame, "Options", true)
        d.setResizable(false)
        p = builder.panel {
            gridBagLayout()
            panel (border : titledBorder(title : trans("OPTIONS_SEARCH_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx: 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx : 100)) {
                gridBagLayout()
                label(text : trans("OPTIONS_SEARCH_IN_COMMENTS"), constraints:gbc(gridx: 0, gridy:0, anchor : GridBagConstraints.LINE_START, 
                    fill : GridBagConstraints.HORIZONTAL, weightx: 100))
                searchCommentsCheckbox = checkBox(selected : bind {model.searchComments}, constraints : gbc(gridx:1, gridy:0,
                anchor : GridBagConstraints.LINE_END, fill : GridBagConstraints.HORIZONTAL, weightx: 0))
                label(text : trans("OPTIONS_SEARCH_IN_COLLECTIONS"), constraints:gbc(gridx: 0, gridy:1, anchor : GridBagConstraints.LINE_START,
                    fill : GridBagConstraints.HORIZONTAL, weightx: 100))
                searchCollectionsCheckbox = checkBox(selected : bind {model.searchCollections}, constraints : gbc(gridx:1, gridy:1,
                anchor : GridBagConstraints.LINE_END, fill : GridBagConstraints.HORIZONTAL, weightx: 0))
                label(text : trans("OPTIONS_ALLOW_BROWSING"), constraints : gbc(gridx : 0, gridy : 2, anchor : GridBagConstraints.LINE_START, 
                    fill : GridBagConstraints.HORIZONTAL, weightx: 100))
                browseFilesCheckbox = checkBox(selected : bind {model.browseFiles}, constraints : gbc(gridx : 1, gridy : 2,
                anchor : GridBagConstraints.LINE_END, fill : GridBagConstraints.HORIZONTAL, weightx: 0))
                label(text : trans("OPTIONS_ALLOW_TRACKING"), constraints : gbc(gridx: 0, gridy: 3, anchor: GridBagConstraints.LINE_START,
                    fill : GridBagConstraints.HORIZONTAL, weightx: 100))
                allowTrackingCheckbox = checkBox(selected : bind {model.allowTracking}, constraints : gbc(gridx: 1, gridy : 3,
                    anchor : GridBagConstraints.LINE_END, fill : GridBagConstraints.HORIZONTAL, weightx : 0))
            }
            
            panel (border : titledBorder(title : trans("OPTIONS_DOWNLOAD_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL, weightx : 100))) {
                gridBagLayout()
                label(text : trans("OPTIONS_RETRY_FAILED_DOWNLOADS"), constraints : gbc(gridx: 0, gridy: 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                retryField = textField(text : bind { model.downloadRetryInterval }, columns : 2, 
                    constraints : gbc(gridx: 2, gridy: 0, anchor : GridBagConstraints.LINE_END, weightx: 0))
                
                label(text : trans("OPTIONS_GIVE_UP_SOURCES"), constraints: gbc(gridx: 0, gridy: 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                downloadMaxFailuresField = textField(text : bind { model.downloadMaxFailures }, columns : 2, 
                    constraints : gbc(gridx: 2, gridy: 1, anchor : GridBagConstraints.LINE_END, weightx: 0))

                if (!isAqua()) {
                    label(text : trans("OPTIONS_SAVE_DOWNLOADED_FILES") + ":", constraints: gbc(gridx:0, gridy:2, anchor : GridBagConstraints.LINE_START))
                    label(text : bind {model.downloadLocation}, constraints: gbc(gridx:1, gridy:2, anchor : GridBagConstraints.LINE_START))
                    button(text : trans("CHOOSE"), constraints : gbc(gridx : 2, gridy:2), downloadLocationAction)

                    label(text : trans("OPTIONS_STORE_INCOMPLETE_FILES") + ":", constraints: gbc(gridx:0, gridy:3, anchor : GridBagConstraints.LINE_START))
                    label(text : bind {model.incompleteLocation}, constraints: gbc(gridx:1, gridy:3, anchor : GridBagConstraints.LINE_START))
                    button(text : trans("CHOOSE"), constraints : gbc(gridx : 2, gridy:3), incompleteLocationAction)
                }
            }
            
            panel (border : titledBorder(title : trans("OPTIONS_UPLOAD_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx : 0, gridy:2, fill : GridBagConstraints.HORIZONTAL, weightx : 100))) {
                gridBagLayout()
                label(text : trans("OPTIONS_TOTAL_UPLOAD_SLOTS"), constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                totalUploadSlotsField = textField(text : bind {model.totalUploadSlots}, columns: 2,
                    constraints : gbc(gridx : 1, gridy: 0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_UPLOAD_SLOTS_PER_USER"), constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                uploadSlotsPerUserField = textField(text : bind {model.uploadSlotsPerUser}, columns: 2,
                    constraints : gbc(gridx : 1, gridy: 1, anchor : GridBagConstraints.LINE_END))
            }
            
            panel (border : titledBorder(title : trans("OPTIONS_SHARING_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx : 0, gridy : 3, fill : GridBagConstraints.HORIZONTAL, weightx : 100))) {
                gridBagLayout()
                label(text : trans("OPTIONS_SHARE_DOWNLOADED_FILES"), constraints : gbc(gridx : 0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                shareDownloadedCheckbox = checkBox(selected : bind {model.shareDownloadedFiles}, constraints : gbc(gridx :1, gridy:0, weightx : 0, anchor: GridBagConstraints.LINE_END))
                
                label(text : trans("OPTIONS_SHARE_HIDDEN_FILES"), constraints : gbc(gridx : 0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                shareHiddenCheckbox = checkBox(selected : bind {model.shareHiddenFiles}, constraints : gbc(gridx :1, gridy:1, weightx : 0, anchor: GridBagConstraints.LINE_END))
                
                label(text : trans("OPTIONS_HASHING_CORES"), constraints : gbc(gridx: 0 , gridy : 2, anchor : GridBagConstraints.LINE_START, weightx : 100))
                hashingCoresTextField = textField(text : bind {model.hashingCores}, columns: 2, 
                    constraints: gbc(gridx: 1, gridy: 2, anchor: GridBagConstraints.LINE_END))
                
                label(text : trans("OPTIONS_IGNORED_FILE_TYPES"), constraints : gbc(gridx: 0, gridy: 3, anchor: GridBagConstraints.LINE_START, weightx : 100))
                ignoredFileTypesTextField = textField(text : bind {model.ignoredFileTypes}, columns: 25,
                        constraints: gbc(gridx: 1, gridy: 3, anchor: GridBagConstraints.LINE_END, fill: GridBagConstraints.HORIZONTAL))
            }
            
            if (!model.disableUpdates) {
                panel (border : titledBorder(title : trans("OPTIONS_UPDATE_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx : 0, gridy : 4, fill : GridBagConstraints.HORIZONTAL, weightx : 100))) {
                    gridBagLayout()
                    label(text : trans("OPTIONS_CHECK_FOR_UPDATES"), constraints : gbc(gridx : 0, gridy: 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                    updateField = textField(text : bind {model.updateCheckInterval }, columns : 2, constraints : gbc(gridx : 1, gridy: 0, weightx: 0))

                    label(text : trans("OPTIONS_DOWNLOAD_UPDATES"), constraints: gbc(gridx :0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                    autoDownloadUpdateCheckbox = checkBox(selected : bind {model.autoDownloadUpdate},
                    constraints : gbc(gridx:1, gridy : 1, anchor : GridBagConstraints.LINE_END))

                }
            }
            panel(constraints : gbc(gridx: 0, gridy: 5, weighty: 100))
        }
        i = builder.panel {
            gridBagLayout()
            label(text : trans("OPTIONS_CHANGING_I2P_SETTINGS"), constraints : gbc(gridx:0, gridy : 0))
            panel (border : titledBorder(title : trans("OPTIONS_TUNNEL_SETTINGS"), border : etchedBorder(), titlePosition: TitledBorder.TOP,
            constraints : gbc(gridx: 0, gridy: 1, fill : GridBagConstraints.HORIZONTAL, weightx : 100))) {
                gridLayout(rows:4, cols:1)
                
                label(text : trans("SPEED_VS_ANONYMITY"))
                def lengthTable = new Hashtable()
                lengthTable.put(1, new JLabel(trans("MAX_SPEED")))
                lengthTable.put(3, new JLabel(trans("MAX_ANONYMITY")))
                tunnelLengthSlider = slider(minimum : 1, maximum : 3, value : bind {model.tunnelLength}, 
                    majorTickSpacing : 1, snapToTicks: true, paintTicks: true, labelTable : lengthTable,
                    paintLabels : true)
                
                
                label(text: trans("RELIABILITY_VS_RESOURCES"))
                def quantityTable = new Hashtable()
                quantityTable.put(1, new JLabel(trans("MIN_RESOURCES")))
                quantityTable.put(6, new JLabel(trans("MAX_RELIABILITY")))
                tunnelQuantitySlider = slider(minimum : 1, maximum : 6, value : bind {model.tunnelQuantity}, 
                    majorTickSpacing : 1, snapToTicks : true, paintTicks: true, labelTable : quantityTable,
                    paintLabels : true)
            }

            Core core = application.context.get("core")
            if (core.router != null) {
                panel(border : titledBorder(title : trans("PORT_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP,
                constraints : gbc(gridx: 0, gridy : 2, fill : GridBagConstraints.HORIZONTAL, weightx: 100))) {
                    gridBagLayout()
                    label(text : trans("TCP_PORT"), constraints : gbc(gridx :0, gridy: 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                    i2pNTCPPortField = textField(text : bind {model.i2pNTCPPort}, columns : 4, constraints : gbc(gridx:1, gridy:0, anchor : GridBagConstraints.LINE_END))
                    label(text : trans("UDP_PORT"), constraints : gbc(gridx :0, gridy: 1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                    i2pUDPPortField = textField(text : bind {model.i2pUDPPort}, columns : 4, constraints : gbc(gridx:1, gridy:1, anchor : GridBagConstraints.LINE_END))
                }
            }
            panel(constraints : gbc(gridx: 0, gridy: 3, weighty: 100))

        }
        u = builder.panel {
            gridBagLayout()
            panel (border : titledBorder(title : trans("OPTIONS_THEME_SETTINGS"), border : etchedBorder(), titlePosition: TitledBorder.TOP,
            constraints : gbc(gridx : 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx : 100))) {
                gridBagLayout()
                label(text : trans("OPTIONS_LOOK_AND_FEEL"), constraints : gbc(gridx: 0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                buttonGroup(id:"lnfGroup")
                radioButton(text: "System", selected : bind {model.systemLnf}, buttonGroup : lnfGroup,
                constraints: gbc(gridx: 1, gridy:0, anchor: GridBagConstraints.LINE_START), systemLnfAction)
                radioButton(text: "Darcula", selected : bind {model.darculaLnf}, buttonGroup : lnfGroup,
                        constraints: gbc(gridx: 2, gridy:0, anchor: GridBagConstraints.LINE_START), darculaLnfAction)
                radioButton(text: "Metal", selected : bind {model.metalLnf}, buttonGroup : lnfGroup,
                        constraints: gbc(gridx: 3, gridy:0, anchor: GridBagConstraints.LINE_START), metalLnfAction)
                label(text : trans("OPTIONS_FONT"), constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                fontField = textField(text : bind {model.font}, columns : 4, constraints : gbc(gridx : 3, gridy:1, anchor : GridBagConstraints.LINE_START))

                label(text : trans("OPTIONS_FONT_SIZE"), constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx : 100))
                buttonGroup(id: "fontSizeGroup")
                radioButton(text: trans("OPTIONS_AUTOMATIC"), selected : bind {model.automaticFontSize}, buttonGroup : fontSizeGroup,
                constraints : gbc(gridx : 1, gridy: 2, anchor : GridBagConstraints.LINE_START), automaticFontAction)
                radioButton(text: trans("OPTIONS_CUSTOM"), selected : bind {!model.automaticFontSize}, buttonGroup : fontSizeGroup,
                constraints : gbc(gridx : 2, gridy: 2, anchor : GridBagConstraints.LINE_START), customFontAction)
                fontSizeField = textField(text : bind {model.customFontSize}, enabled : bind {!model.automaticFontSize}, 
                    constraints : gbc(gridx : 3, gridy : 2, anchor : GridBagConstraints.LINE_END))
                
                label(text : trans("OPTIONS_FONT_STYLE"), constraints: gbc(gridx: 0, gridy: 3, anchor : GridBagConstraints.LINE_START, weightx: 100))
                panel(constraints : gbc(gridx: 2, gridy: 3, gridwidth: 2, anchor:GridBagConstraints.LINE_END)) {
                    fontStyleBoldCheckbox = checkBox(selected : bind {model.fontStyleBold})
                    label(text: trans("OPTIONS_BOLD"))
                    fontStyleItalicCheckbox = checkBox(selected : bind {model.fontStyleItalic})
                    label(text: trans("OPTIONS_ITALIC"))
                }

            }
            panel (border : titledBorder(title : trans("OPTIONS_SEARCH_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL, weightx : 100)) {
                gridBagLayout()
                label(text : trans("OPTIONS_DEFAULT_GROUP_RESULTS"), constraints : gbc(gridx :0, gridy: 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                panel(constraints : gbc(gridx : 1, gridy : 0, anchor : GridBagConstraints.LINE_END)) {
                    buttonGroup(id : "groupBy")
                    radioButton(text : trans("SENDER"), selected : bind {!model.groupByFile}, buttonGroup : groupBy, groupBySenderAction)
                    radioButton(text : trans("FILE"), selected : bind {model.groupByFile}, buttonGroup : groupBy, groupByFileAction)
                }
                label(text : trans("OPTIONS_REMEMBER_HISTORY"), constraints: gbc(gridx: 0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                storeSearchHistoryCheckbox = checkBox(selected : bind {model.storeSearchHistory},
                constraints : gbc(gridx : 1, gridy:1, anchor : GridBagConstraints.LINE_END))
                button(text : trans("OPTIONS_CLEAR_HISTORY"), constraints : gbc(gridx : 1, gridy : 2, anchor : GridBagConstraints.LINE_END), clearHistoryAction)
                
            }
            panel (border : titledBorder(title : trans("OPTIONS_OTHER_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 2, fill : GridBagConstraints.HORIZONTAL, weightx : 100)) {
                gridBagLayout()
                label(text : trans("OPTIONS_CLEAR_CANCELLED_DOWNLOADS"), constraints: gbc(gridx: 0, gridy:0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                clearCancelledDownloadsCheckbox = checkBox(selected : bind {model.clearCancelledDownloads},
                constraints : gbc(gridx : 1, gridy:0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_CLEAR_FINISHED_DOWNLOADS"), constraints: gbc(gridx: 0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                clearFinishedDownloadsCheckbox = checkBox(selected : bind {model.clearFinishedDownloads},
                constraints : gbc(gridx : 1, gridy:1, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_SMOOTH_DOWNLOAD_SPEED"), constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx: 100))
                speedSmoothSecondsField = textField(text : bind {model.speedSmoothSeconds},
                constraints : gbc(gridx:1, gridy: 2, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_EXCLUDE_LOCAL_FILES"), constraints: gbc(gridx:0, gridy:3, anchor : GridBagConstraints.LINE_START, weightx: 100))
                excludeLocalResultCheckbox = checkBox(selected : bind {model.excludeLocalResult},
                constraints : gbc(gridx: 1, gridy : 3, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_CLEAR_FINISHED_UPLOADS"), constraints:gbc(gridx:0, gridy:4, anchor: GridBagConstraints.LINE_START, weightx : 100))
                clearUploadsCheckbox = checkBox(selected : bind {model.clearUploads},
                constraints : gbc(gridx:1, gridy: 4, anchor:GridBagConstraints.LINE_END))

                if (SystemTray.isSupported()) {
                    label(text : trans("OPTIONS_WHEN_CLOSING_MUWIRE"), constraints : gbc(gridx: 0, gridy : 5, anchor : GridBagConstraints.LINE_START, weightx: 100))
                    panel (constraints : gbc(gridx:1, gridy: 5, anchor : GridBagConstraints.LINE_END)) {
                        buttonGroup(id : "closeBehaviorGroup")
                        radioButton(text : trans("OPTIONS_MINIMIZE_TO_TRAY"), selected : bind {!model.exitOnClose}, buttonGroup: closeBehaviorGroup, minimizeOnCloseAction)
                        radioButton(text : trans("EXIT"), selected : bind {model.exitOnClose}, buttonGroup : closeBehaviorGroup, exitOnCloseAction)
                    }
                }
            }
            panel (constraints : gbc(gridx: 0, gridy: 3, weighty: 100))
        }
        bandwidth = builder.panel {
            gridBagLayout()
            panel( border : titledBorder(title : trans("OPTIONS_BANDWIDTH_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx : 100)) {
                gridBagLayout()
                label(text : trans("INBOUND_BANDWIDTH"), constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx : 100))
                inBwField = textField(text : bind {model.inBw}, columns : 3, constraints : gbc(gridx : 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OUTBOUND_BANDWIDTH"), constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                outBwField = textField(text : bind {model.outBw}, columns : 3, constraints : gbc(gridx : 1, gridy : 1, anchor : GridBagConstraints.LINE_END))
            }
            panel(constraints : gbc(gridx: 0, gridy: 1, weighty: 100))
        }
        feed = builder.panel {
            gridBagLayout()
            panel (border : titledBorder(title : trans("OPTIONS_FEED_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
                constraints : gbc(gridx : 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("OPTIONS_ENABLE_FEED"), constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                fileFeedCheckbox = checkBox(selected : bind {model.fileFeed}, constraints : gbc(gridx: 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_ADVERTISE_FEED"), constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                advertiseFeedCheckbox = checkBox(selected : bind {model.advertiseFeed}, constraints : gbc(gridx: 1, gridy : 1, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_PUBLISH_SHARED"), constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx: 100))
                autoPublishSharedFilesCheckbox = checkBox(selected : bind {model.autoPublishSharedFiles}, constraints : gbc(gridx: 1, gridy : 2, anchor : GridBagConstraints.LINE_END))
            }
            panel (border : titledBorder(title : trans("OPTIONS_NEW_FEED_DEFAULTS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
                constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("OPTIONS_FEED_AUTO_DOWNLOAD"), constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                defaultFeedAutoDownloadCheckbox = checkBox(selected : bind {model.defaultFeedAutoDownload}, constraints : gbc(gridx: 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_FEED_DOWNLOAD_SEQUENTIALLY"), constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                defaultFeedSequentialCheckbox = checkBox(selected : bind {model.defaultFeedSequential}, constraints : gbc(gridx: 1, gridy : 1, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_FEED_ITEMS_ON_DISK"), constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx: 100))
                defaultFeedItemsToKeepField = textField(text : bind {model.defaultFeedItemsToKeep}, constraints:gbc(gridx :1, gridy:2, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_FEED_REFRESH_FREQUENCY"), constraints : gbc(gridx: 0, gridy : 3, anchor : GridBagConstraints.LINE_START, weightx: 100))
                defaultFeedUpdateIntervalField = textField(text : bind {model.defaultFeedUpdateInterval}, constraints:gbc(gridx :1, gridy:3, anchor : GridBagConstraints.LINE_END))
            }
            panel(constraints : gbc(gridx: 0, gridy : 2, weighty: 100))
        }
        trust = builder.panel {
            gridBagLayout()
            panel (border : titledBorder(title : trans("OPTIONS_TRUST_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
            constraints : gbc(gridx : 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("OPTIONS_ONLY_TRUSTED"), constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                allowUntrustedCheckbox = checkBox(selected : bind {model.onlyTrusted}, constraints : gbc(gridx: 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_SEARCH_EXTRA_HOP"), constraints : gbc(gridx:0, gridy:1, anchor : GridBagConstraints.LINE_START, weightx : 100))
                searchExtraHopCheckbox = checkBox(selected : bind {model.searchExtraHop}, constraints : gbc(gridx: 1, gridy : 1, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_ALLOW_TRUST_LIST"), constraints : gbc(gridx: 0, gridy : 2, anchor : GridBagConstraints.LINE_START, weightx : 100))
                allowTrustListsCheckbox = checkBox(selected : bind {model.trustLists}, constraints : gbc(gridx: 1, gridy : 2, anchor : GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_TRUST_LIST_UPDATE_INTERVAL"), constraints : gbc(gridx:0, gridy:3, anchor : GridBagConstraints.LINE_START, weightx : 100))
                trustListIntervalField = textField(text : bind {model.trustListInterval}, constraints:gbc(gridx:1, gridy:3, anchor : GridBagConstraints.LINE_END))
            }
            panel(constraints : gbc(gridx: 0, gridy : 1, weighty: 100))
        }
        
        chat = builder.panel {
            gridBagLayout()
            panel (border : titledBorder(title : trans("OPTIONS_CHAT_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
                constraints : gbc(gridx : 0, gridy : 0, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("OPTIONS_START_CHAT_SERVER_STARTUP"), constraints : gbc(gridx: 0, gridy: 0, anchor: GridBagConstraints.LINE_START, weightx: 100))
                startChatServerCheckbox = checkBox(selected : bind{model.startChatServer}, constraints : gbc(gridx:2, gridy:0, anchor:GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_MAX_CHAT_CONNECTIONS"), constraints : gbc(gridx: 0, gridy:1, anchor:GridBagConstraints.LINE_START, weightx:100))
                maxChatConnectionsField = textField(text : bind {model.maxChatConnections}, constraints : gbc(gridx: 2, gridy : 1, anchor:GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_ADVERTISE_CHAT"), constraints : gbc(gridx: 0, gridy:2, anchor:GridBagConstraints.LINE_START, weightx:100))
                advertiseChatCheckbox = checkBox(selected : bind{model.advertiseChat}, constraints : gbc(gridx:2, gridy:2, anchor:GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_MAX_CHAT_SCROLLBACK"), constraints : gbc(gridx:0, gridy:3, anchor : GridBagConstraints.LINE_START, weightx: 100))
                maxChatLinesField = textField(text : bind{model.maxChatLines}, constraints : gbc(gridx:2, gridy: 3, anchor: GridBagConstraints.LINE_END))
                if (!isAqua()) {
                    label(text : trans("OPTIONS_CHAT_WELCOME_FILE"), constraints : gbc(gridx : 0, gridy : 4, anchor : GridBagConstraints.LINE_START, weightx: 100))
                    label(text : bind {model.chatWelcomeFile}, constraints : gbc(gridx : 1, gridy : 4))
                    button(text : trans("CHOOSE"), constraints : gbc(gridx : 2, gridy : 4, anchor : GridBagConstraints.LINE_END), chooseChatFileAction)
                }
            }
            panel (border : titledBorder(title : trans("OPTIONS_MESSAGING_SETTINGS"), border : etchedBorder(), titlePosition : TitledBorder.TOP),
                constraints : gbc(gridx : 0, gridy : 1, fill : GridBagConstraints.HORIZONTAL, weightx: 100)) {
                gridBagLayout()
                label(text : trans("OPTIONS_ALLOW_MESSAGES"), constraints : gbc(gridx: 0, gridy: 0, anchor: GridBagConstraints.LINE_START, weightx: 100))
                allowMessagesCheckbox = checkBox(selected : bind{model.allowMessages}, constraints : gbc(gridx:2, gridy:0, anchor:GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_ALLOW_TRUSTED_MESSAGES"), constraints : gbc(gridx: 0, gridy: 1, anchor: GridBagConstraints.LINE_START, weightx: 100))
                allowOnlyTrustedMessagesCheckbox = checkBox(selected : bind{model.allowOnlyTrustedMessages}, constraints : gbc(gridx:2, gridy:1, anchor:GridBagConstraints.LINE_END))
                label(text : trans("OPTIONS_MESSAGE_SEND_INTERVAL"), constraints : gbc(gridx: 0, gridy: 2, anchor: GridBagConstraints.LINE_START, weightx: 100))
                messageSendIntervalField = textField(text : bind{model.messageSendInterval}, constraints : gbc(gridx : 2, gridy : 2, anchor : GridBagConstraints.LINE_END))
                
                if (Taskbar.isTaskbarSupported() || SystemTray.isSupported()) {
                    label(text : trans("OPTIONS_MESSAGE_NOTIFICATIONS"), constraints : gbc(gridx: 0, gridy: 3, anchor: GridBagConstraints.LINE_START, weightx: 100))
                    messageNotificationsCheckbox = checkBox(selected : bind{model.messageNotifications}, constraints : gbc(gridx:2, gridy:3, anchor:GridBagConstraints.LINE_END))
                }
            }
            panel(constraints : gbc(gridx: 0, gridy : 2, weighty: 100))
        }


        buttonsPanel = builder.panel {
            gridBagLayout()
            button(text : trans("SAVE"), constraints : gbc(gridx : 1, gridy: 2), saveAction)
            button(text : trans("CANCEL"), constraints : gbc(gridx : 2, gridy: 2), cancelAction)
        }
    }

    boolean isAqua() {
        // this is a Java bug.  File choosers don't appear on Aqua L&F.
        model.systemLnf && SystemVersion.isMac()
    }

    void mvcGroupInit(Map<String,String> args) {
        def tabbedPane = new JTabbedPane()
        tabbedPane.addTab("MuWire", p)
        tabbedPane.addTab("I2P", i)
        tabbedPane.addTab(trans("GUI"), u)
        Core core = application.context.get("core")
        if (core.router != null) {
            tabbedPane.addTab(trans("BANDWIDTH"), bandwidth)
        }
        tabbedPane.addTab(trans("FEED"), feed)
        tabbedPane.addTab(trans("TRUST_NOUN"), trust)
        tabbedPane.addTab(trans("COMMUNICATIONS"), chat)

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