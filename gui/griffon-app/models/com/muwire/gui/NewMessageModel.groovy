package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.EventBus
import com.muwire.core.Persona
import com.muwire.core.messenger.MWMessage

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import net.i2p.data.SigningPrivateKey
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class NewMessageModel {
    Core core
    MWMessage reply
    Persona recipient
}