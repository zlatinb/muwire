package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JFileChooser
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.border.Border

import com.muwire.core.download.Downloader
import com.muwire.core.files.FileSharedEvent

import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.nio.charset.StandardCharsets

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class MainFrameView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    MainFrameModel model
    
    void initUI() {
        builder.with {
            application(size : [1024,768], id: 'main-frame',
            locationRelativeTo : null,
            title: application.configuration['application.title'],
            iconImage:   imageIcon('/griffon-icon-48x48.png').image,
            iconImages: [imageIcon('/griffon-icon-48x48.png').image,
                imageIcon('/griffon-icon-32x32.png').image,
                imageIcon('/griffon-icon-16x16.png').image],
            pack : false,
            visible : bind { model.coreInitialized }) {
                borderLayout()
                panel (border: etchedBorder(), constraints : BorderLayout.NORTH) {
                    borderLayout()
                    panel (constraints: BorderLayout.WEST) {
                        gridLayout(rows:1, cols: 2)
                        button(text: "Searches", actionPerformed : showSearchWindow)
                        button(text: "Uploads", actionPerformed : showUploadsWindow)
                        button(text: "Monitor", actionPerformed : showMonitorWindow)
                        button(text: "Trust", actionPerformed : showTrustWindow)
                    }
                    panel(id: "top-panel", constraints: BorderLayout.CENTER) {
                        cardLayout()
                        label(constraints : "top-connect-panel",
                            text : "        MuWire is connecting, please wait.  You will be able to search soon.") // TODO: real padding
                        panel(constraints : "top-search-panel") {
                            borderLayout()
                            panel(constraints: BorderLayout.CENTER) {
                                borderLayout()
                                label("        Enter search here:", constraints: BorderLayout.WEST) // TODO: fix this
                                textField(id: "search-field", constraints: BorderLayout.CENTER, action : searchAction)

                            }
                            panel( constraints: BorderLayout.EAST) {
                                button(text: "Search", searchAction)
                            }
                        }
                    }
                }
                panel (id: "cards-panel", constraints : BorderLayout.CENTER) {
                    cardLayout()
                    panel (constraints : "search window") {
                        borderLayout()
                        splitPane( orientation : JSplitPane.VERTICAL_SPLIT, dividerLocation : 500,
                        continuousLayout : true, constraints : BorderLayout.CENTER) {
                            panel (constraints : JSplitPane.TOP) {
                                borderLayout()
                                tabbedPane(id : "result-tabs", constraints: BorderLayout.CENTER)
                                panel(constraints : BorderLayout.SOUTH) {
                                    button(text : "Download", enabled : bind {model.searchButtonsEnabled}, downloadAction)
                                    button(text : "Trust", enabled: bind {model.searchButtonsEnabled }, trustAction)
                                    button(text : "Distrust", enabled : bind {model.searchButtonsEnabled}, distrustAction)
                                }
                            }
                            panel (constraints : JSplitPane.BOTTOM) {
                                borderLayout()
                                scrollPane (constraints : BorderLayout.CENTER) {
                                    table(id : "downloads-table") {
                                        tableModel(list: model.downloads) {
                                            closureColumn(header: "Name", type: String, read : {row -> row.downloader.file.getName()})
                                            closureColumn(header: "Status", type: String, read : {row -> row.downloader.getCurrentState()})
                                            closureColumn(header: "Progress", type: String, read: { row ->
                                                int pieces = row.downloader.nPieces
                                                int done = row.downloader.donePieces()
                                                "$done/$pieces pieces"
                                            })
                                            closureColumn(header: "Piece", type: String, read: { row ->
                                                int position = row.downloader.positionInPiece()
                                                int pieceSize = row.downloader.pieceSize // TODO: fix for last piece
                                                "$position/$pieceSize bytes"
                                            })
                                        }
                                    }
                                }
                                panel (constraints : BorderLayout.SOUTH) {
                                    button(text: "Cancel", enabled : bind {model.cancelButtonEnabled }, cancelAction )
                                    button(text: "Retry", enabled : bind {model.retryButtonEnabled}, resumeAction)
                                }
                            }
                        }
                    }
                    panel (constraints: "uploads window"){
                        gridLayout(cols : 1, rows : 2)
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH) {
                                button(text : "Click here to share files", actionPerformed : shareFiles)
                            }
                            scrollPane ( constraints : BorderLayout.CENTER) {
                                table(id : "shared-files-table") {
                                     tableModel(list : model.shared) {
                                         closureColumn(header : "Name", type : String, read : {row -> row.file.getAbsolutePath()})
                                         closureColumn(header : "Size", type : Long, read : {row -> row.file.length()})
                                     }   
                                }
                            }
                        }
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH){
                                label("Uploads")
                            }
                            scrollPane (constraints : BorderLayout.CENTER) {
                                table(id : "uploads-table") {
                                    tableModel(list : model.uploads) {
                                        closureColumn(header : "Name", type : String, read : {row -> row.file.getName() })
                                        closureColumn(header : "Progress", type : String, read : { row ->
                                            int position = row.getPosition()
                                            def range = row.request.getRange()
                                            int total = range.end - range.start
                                            int percent = (int)((position * 100.0) / total)
                                            "$percent%"
                                        })
                                        closureColumn(header : "Downloader", type : String, read : { row -> 
                                            row.request.downloader?.getHumanReadableName()
                                        })
                                    }
                                }
                            }
                        }
                    }
                    panel (constraints: "monitor window") {
                        gridLayout(rows : 1, cols : 2)
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH){
                                label("Connections")
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "connections-table") {
                                    tableModel(list : model.connectionList) {
                                        closureColumn(header : "Destination", type: String, read : { row -> row.toBase32() })
                                    }
                                }
                            }
                        }
                        panel {
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH){
                                label("Incoming searches")
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "searches-table") {
                                    tableModel(list : model.searches) {
                                        closureColumn(header : "Keywords", type : String, read : { it })
                                    }
                                }
                            }
                        }
                    }
                    panel(constraints : "trust window") {
                        gridLayout(rows: 1, cols :2)
                        panel (border : etchedBorder()){
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH) {
                                label("Trusted users")
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "trusted-table") {
                                    tableModel(list : model.trusted) {
                                        closureColumn(type : String, read : { it.getHumanReadableName() } )
                                    }
                                }
                            }
                            panel (constraints : BorderLayout.EAST) {
                                gridBagLayout()
                                button(text : "Mark Neutral", constraints : gbc(gridx: 0, gridy: 0), markNeutralFromTrustedAction)
                                button(text : "Mark Distrusted", constraints : gbc(gridx: 0, gridy:1), markDistrustedAction)
                            }
                        }
                        panel (border : etchedBorder()){
                            borderLayout()
                            panel (constraints : BorderLayout.NORTH) {
                                label("Distrusted users")
                            }
                            scrollPane(constraints : BorderLayout.CENTER) {
                                table(id : "distrusted-table") {
                                    tableModel(list : model.distrusted) {
                                        closureColumn(type : String, read : { it.getHumanReadableName() } )
                                    }
                                }
                            }
                            panel(constraints : BorderLayout.WEST) {
                                gridBagLayout()
                                button(text: "Mark Neutral", constraints: gbc(gridx: 0, gridy: 0), markNeutralFromDistrustedAction)
                                button(text: "Mark Trusted", constraints : gbc(gridx: 0, gridy : 1), markTrustedAction)
                            }
                        }
                    }
                }
                panel (border: etchedBorder(), constraints : BorderLayout.SOUTH) {
                    borderLayout()
                    label(text : bind {model.me}, constraints: BorderLayout.CENTER)
                    panel (constraints : BorderLayout.EAST) {
                        label("Connections:")
                        label(text : bind {model.connections})
                    }
                }

            }
        }
    }
    
    void mvcGroupInit(Map<String, String> args) {
        def downloadsTable = builder.getVariable("downloads-table")
        def selectionModel = downloadsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = downloadsTable.getSelectedRow()
            def downloader = model.downloads[selectedRow].downloader
            switch(downloader.getCurrentState()) {
                case Downloader.DownloadState.CONNECTING :
                case Downloader.DownloadState.DOWNLOADING :
                model.cancelButtonEnabled = true
                model.retryButtonEnabled = false
                break
                case Downloader.DownloadState.FAILED:
                model.cancelButtonEnabled = false
                model.retryButtonEnabled = true
                break
                default:
                model.cancelButtonEnabled = false
                model.retryButtonEnabled = false
            }
        })
    }

    def showSearchWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "search window")
    }

    def showUploadsWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "uploads window")
    }
    
    def showMonitorWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel,"monitor window")
    }
    
    def showTrustWindow = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel,"trust window")
    }
    
    def shareFiles = {
        def chooser = new JFileChooser()
        chooser.setDialogTitle("Select file or directory to share")
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES)
        int rv = chooser.showOpenDialog(null)
        if (rv == JFileChooser.APPROVE_OPTION) {
            model.core.eventBus.publish(new FileSharedEvent(file : chooser.getSelectedFile()))
        }
    }
}