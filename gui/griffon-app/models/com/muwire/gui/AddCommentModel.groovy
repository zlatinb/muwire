package com.muwire.gui

import com.muwire.core.SharedFile

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class AddCommentModel {
    List<SharedFile> selectedFiles
}