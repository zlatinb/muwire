package com.muwire.gui

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class ContentPanelModel {
    @Observable int clickCount = 0
}