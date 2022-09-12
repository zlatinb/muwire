package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class LibrarySyncController {

    @MVCMember @Nonnull
    LibrarySyncModel model
    @MVCMember @Nonnull
    LibrarySyncView view


    @ControllerAction
    void cancelScan() {
        model.cancelScan()
        view.scanCancelled()
    }

    @ControllerAction
    void cancel() {
        view.previewCancelled()
    }

    @ControllerAction
    void reindex() {

    }
}
