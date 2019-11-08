package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JTabbedPane
import javax.swing.SwingConstants

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class SharedFileView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    SharedFileModel model

    def mainFrame
    def dialog
    def panel
    def searchersPanel
    def downloadersPanel
    def certificatesTable
    def certificatesPanel
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")
        dialog = new JDialog(mainFrame,"Details for "+model.sf.getFile().getName(),true)
        dialog.setResizable(true)
        
        searchersPanel = builder.panel {
            borderLayout()
            scrollPane(constraints : BorderLayout.CENTER) {
                table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.searchers) {
                        closureColumn(header : "Searcher", type : String, read : {it.searcher?.getHumanReadableName()})
                        closureColumn(header : "Query", type : String, read : {it.query})
                        closureColumn(header : "Timestamp", type : String, read : {
                            Date d = new Date(it.timestamp)
                            d.toString()
                        })
                    }
                }
            }
        }
        
        downloadersPanel = builder.panel {
            borderLayout()
            scrollPane(constraints : BorderLayout.CENTER) {
                table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.downloaders) {
                        closureColumn(header : "Downloader", type : String, read : {it})
                    }
                }
            }
        }
        
        certificatesPanel = builder.panel {
            borderLayout()
            scrollPane(constraints : BorderLayout.CENTER) {
                certificatesTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list : model.certificates) {
                        closureColumn(header : "Issuer", type:String, read : {it.issuer.getHumanReadableName()})
                        closureColumn(header : "File Name", type : String, read : {it.name.name})
                        closureColumn(header : "Comment", type : Boolean, read : {it.comment != null})
                        closureColumn(header :  "Timestamp", type : String, read : {
                            Date d = new Date(it.timestamp)
                            d.toString()
                        })
                    }
                }
            }
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        def tabbedPane = new JTabbedPane()
        tabbedPane.addTab("Search Hits", searchersPanel)
        tabbedPane.addTab("Downloaders", downloadersPanel)
        tabbedPane.addTab("Certificates", certificatesPanel)
        
        dialog.with { 
            getContentPane().add(tabbedPane)
            pack()
            setLocationRelativeTo(mainFrame)
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
            addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                    mvcGroup.destroy()
                }
            })
            show()
        }
    }
}