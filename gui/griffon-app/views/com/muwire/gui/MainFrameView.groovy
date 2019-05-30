package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.SwingConstants
import javax.swing.border.Border

import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets

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
                    panel (border: etchedBorder(), constraints: BorderLayout.WEST) {
                        gridLayout(rows:1, cols: 2)
                        button(text: "1", actionPerformed : showCard1)
                        button(text: "2", actionPerformed : showCard2)
                    }
                    panel(constraints: BorderLayout.CENTER) {
                        borderLayout()
                        label("Enter search here:", constraints: BorderLayout.WEST)
                        textField(constraints: BorderLayout.CENTER)
                    }
                    panel( border: etchedBorder(), constraints: BorderLayout.EAST) {
                        button("Search")
                    }
                }
                panel (id: "cards-panel", border: etchedBorder(), constraints : BorderLayout.CENTER) {
                    cardLayout()
                    panel (constraints : "card1") {
                        label("card 1")
                    }
                    panel (constraints: "card2"){
                        label("card 2")
                    }
                }
                panel (border: etchedBorder(), constraints : BorderLayout.SOUTH) {
                    
                }
            }
        }
    }
    
    def showCard1 = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "card1")
    }
    
    def showCard2 = {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "card2")
    }
}