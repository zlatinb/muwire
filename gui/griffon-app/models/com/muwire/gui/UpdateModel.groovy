package com.muwire.gui

import com.muwire.core.update.UpdateAvailableEvent
import com.muwire.core.update.UpdateDownloadedEvent

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class UpdateModel {
    UpdateAvailableEvent available
    UpdateDownloadedEvent downloaded
}