package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.SwingConstants
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class EventListView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    EventListModel model

    void initUI() {
        builder.with {
            application(size: [320, 80], id: 'event-list',
                locationRelativeTo : null,
                title: application.configuration['application.title'],
                iconImage:   imageIcon('/MuWire-48x48.png').image,
                iconImages: [imageIcon('/MuWire-48x48.png').image,
                             imageIcon('/MuWire-32x32.png').image,
                             imageIcon('/MuWire-16x16.png').image],
                 visible: bind { !model.coreInitialized} ) {
                panel {
                    vbox {
                        label("MuWire is loading, please wait...")
                        Box.createVerticalGlue()
                        progressBar(indeterminate : true)
                    }
                }
            }
        }
    }
}