package com.muwire.gui

import javax.annotation.Nonnull

import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class SystemStatusModel {
    @MVCMember @Nonnull
    SystemStatusController controller
    
    @Observable String javaVendor
    @Observable String javaVersion
    @Observable long usedRam
    @Observable long totalRam
    @Observable long maxRam
    
    void mvcGroupInit(Map<String,String> args) {
        controller.refresh()
    }
}