
package com.muwire.gui

import griffon.core.artifact.GriffonView

import javax.swing.JFrame

import static com.muwire.gui.Translator.trans
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.SwingConstants
import javax.swing.event.ChangeListener

import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class WatchedDirectoryView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    WatchedDirectoryModel model

    JFrame window
    def mainFrame
    
    def autoWatchCheckbox
    def syncIntervalField
    def applySubCheckbox

    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        
        window = builder.frame(visible: false, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
            iconImage: builder.imageIcon("/MuWire-48x48.png").image,
            title: trans("WATCHED_DIRECTORY_CONFIGURATION")) {
            borderLayout()
            panel (constraints : BorderLayout.NORTH) {
                label(trans("WATCHED_DIRECTORY_CONFIGURATION_FOR",model.directory.directory.toString()))
            }
            panel (constraints : BorderLayout.CENTER) {
                gridBagLayout()
                
                label(text : trans("WATCHED_DIRECTORY_AUTO"), toolTipText: trans("TOOLTIP_TOOLS_FOLDER_AUTO_WATCH"), 
                        constraints : gbc(gridx: 0, gridy : 0, anchor : GridBagConstraints.LINE_START, weightx: 100))
                autoWatchCheckbox = checkBox(selected : bind {model.autoWatch}, constraints : gbc(gridx: 1, gridy : 0, anchor : GridBagConstraints.LINE_END))
                
                label(text : trans("WATCHED_DIRECTORY_INTERVAL"), toolTipText: trans("TOOLTIP_TOOLS_FOLDER_SYNC_FREQUENCY"),
                        enabled : bind {!model.autoWatch}, constraints : gbc(gridx: 0, gridy : 1, anchor : GridBagConstraints.LINE_START, weightx: 100))
                syncIntervalField = textField(text : bind {model.syncInterval}, columns: 4, enabled : bind {!model.autoWatch}, 
                    constraints: gbc(gridx: 1, gridy : 1, anchor : GridBagConstraints.LINE_END, insets : [0,10,0,0]))
            }
            panel (constraints : BorderLayout.SOUTH) {
                gridLayout(rows: 1, cols: 3)
                panel()
                panel() {
                    button(text: trans("SAVE"), saveAction)
                    button(text: trans("CANCEL"), cancelAction)
                } 
                panel() {
                    label(text : trans("WATCHED_DIRECTORY_APPLY_SUB"), toolTipText: trans("TOOLTIP_TOOLS_FOLDER_APPLY_SUBFOLDERS"))
                    applySubCheckbox = checkBox(selected: false)
                }
            }
        }
    }
        
    void mvcGroupInit(Map<String,String> args) {
        autoWatchCheckbox.addChangeListener({e ->
            model.autoWatch = autoWatchCheckbox.model.isSelected()
        } as ChangeListener)

        window.addWindowListener( new WindowAdapter() {
            void windowClosed(WindowEvent event) {
                mvcGroup.destroy()
            }
        })
        window.pack()
        window.setLocationRelativeTo(mainFrame)
        window.setVisible(true)
    }
}