package com.muwire.gui

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class OptionsModel {
    @Observable String downloadRetryInterval 
    
    void mvcGroupInit(Map<String, String> args) {
        downloadRetryInterval = application.context.get("muwire-settings").downloadRetryInterval
    }
}