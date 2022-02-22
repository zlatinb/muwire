package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.update.UpdateAvailableEvent
import com.muwire.core.update.UpdateDownloadedEvent

import griffon.core.artifact.GriffonModel
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class UpdateModel {
    Core core
    UpdateAvailableEvent available
    UpdateDownloadedEvent downloaded
}