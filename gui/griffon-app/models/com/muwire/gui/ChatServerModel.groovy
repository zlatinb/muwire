package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.Persona

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class ChatServerModel {
    Persona host
    Core core
    
    @Observable boolean disconnectActionEnabled
    
    
    void mvcGroupInit(Map<String, String> params) {
        disconnectActionEnabled = host != core.me // can't disconnect from myself
    }
}