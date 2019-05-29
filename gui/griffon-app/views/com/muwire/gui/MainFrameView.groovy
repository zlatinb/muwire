package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.swing.SwingConstants
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class MainFrameView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    MainFrameModel model

    void initUI() {
        builder.with {
            application(size: [1024, 768], id: 'main-frame',
                title: application.configuration['application.title'],
                iconImage:   imageIcon('/griffon-icon-48x48.png').image,
                iconImages: [imageIcon('/griffon-icon-48x48.png').image,
                             imageIcon('/griffon-icon-32x32.png').image,
                             imageIcon('/griffon-icon-16x16.png').image],
                visible : bind { model.coreInitialized }) {
                gridLayout(rows: 2, cols: 1)
                label(id: 'clickLabel', text: bind { model.clickCount },
                     horizontalAlignment: SwingConstants.CENTER)
                button(id: 'clickButton', clickAction)
            }
        }
    }
}