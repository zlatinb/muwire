package com.muwire.gui

import com.muwire.core.collections.FileCollection

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class CollectionWarningModel {
    String[] collections
    String fileName
    boolean [] answer
    UISettings settings
    File home
}