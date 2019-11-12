package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.Persona

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class ChatRoomModel {
    Core core
    Persona host
    String tabName
    String room
    boolean console
    
    def members = []
    
    void mvcGroupInit(Map<String,String> args) {
        members.add(core.me)
    }
}