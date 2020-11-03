package com.muwire.gui

import com.muwire.core.Persona
import com.muwire.core.messenger.MWMessage

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class NewMessageModel {
    MWMessage reply
    Persona recipient
}