package com.muwire.gui

import griffon.core.GriffonApplication
import static com.muwire.gui.Translator.trans
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.Box
import javax.swing.SwingConstants
import javax.annotation.Nonnull
import javax.inject.Inject

@ArtifactProviderFor(GriffonView)
class ShutdownWindowView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ShutdownWindowModel model
    
    void initUI() {
        builder.with {
            application(size: [320, 80], id: 'shutdown-window',
                    locationRelativeTo : null,
                    pack: true,
                    title: application.configuration['application.title'],
                    iconImage:   imageIcon('/MuWire-48x48.png').image,
                    iconImages: [imageIcon('/MuWire-48x48.png').image,
                                 imageIcon('/MuWire-32x32.png').image,
                                 imageIcon('/MuWire-16x16.png').image],
                    visible: false ) {
                panel {
                    vbox {
                        label(trans("MUWIRE_IS_CLOSING"))
                        Box.createVerticalGlue()
                        progressBar(indeterminate : true)
                    }
                }
            }
        }
    }
}