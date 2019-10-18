package com.muwire.gui

import com.muwire.core.Persona

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

import com.muwire.core.search.BrowseStatus

@ArtifactProviderFor(GriffonModel)
class BrowseModel {
    Persona host
    @Observable BrowseStatus status
    @Observable boolean downloadActionEnabled
    @Observable boolean viewCommentActionEnabled
    
    def results = []
}