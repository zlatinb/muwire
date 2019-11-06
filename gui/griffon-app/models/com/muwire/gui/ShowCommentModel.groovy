package com.muwire.gui

import com.muwire.core.search.UIResultEvent

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class ShowCommentModel {
    String name
    String text
}