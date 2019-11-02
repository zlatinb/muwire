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
class AdvancedSharingView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    AdvancedSharingModel model

    def mainFrame
    def dialog
    def watchedDirsPanel
    def negativeTreePanel
    
    def watchedDirsTable
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame,"Advanced Sharing",true)
        dialog.setResizable(true)
        
        watchedDirsPanel = builder.panel {
            borderLayout()
            panel (constraints : BorderLayout.NORTH) {
                label(text : "Directories watched for file changes")
            }
            scrollPane( constraints : BorderLayout.CENTER ) {
                watchedDirsTable = table(autoCreateRowSorter : true) {
                    tableModel(list : model.watchedDirectories) {
                        closureColumn(header : "Directory", type : String, read : {it})
                    }
                }
            }
        }
        
        negativeTreePanel = builder.panel {
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                label(text : "Files which are explicitly not shared")
            }
            scrollPane( constraints : BorderLayout.CENTER ) {
                // add tree here
            }
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        def tabbedPane = new JTabbedPane()
        tabbedPane.addTab("Watched Directories", watchedDirsPanel)
        tabbedPane.addTab("Negative Tree", negativeTreePanel)
        
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