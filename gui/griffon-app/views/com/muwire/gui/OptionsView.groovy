package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingConstants

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
    def retryField
    def updateField
    def allowUntrustedCheckbox
    
    def inboundLengthField
    def inboundQuantityField
    def outboundLengthField
    def outboundQuantityField

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

            label(text : "Allow untrusted connections", constraints : gbc(gridx: 0, gridy : 2))
            allowUntrustedCheckbox = checkBox(selected : bind {model.allowUntrusted}, constraints : gbc(gridx: 1, gridy : 2))
                        
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
        }
        buttonsPanel = builder.panel {
            gridBagLayout()
            button(text : "Save", constraints : gbc(gridx : 1, gridy: 2), saveAction)
            button(text : "Cancel", constraints : gbc(gridx : 2, gridy: 2), cancelAction)
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        def tabbedPane = new JTabbedPane()
        tabbedPane.addTab("MuWire Options", p)
        tabbedPane.addTab("I2P Options", i)
                
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