package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.files.directories.WatchedDirectory

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class WatchedDirectoryModel {
    Core core
    WatchedDirectory directory
    
    @Observable boolean autoWatch
    @Observable int syncInterval
    
    void mvcGroupInit(Map<String,String> args) {
        autoWatch = directory.autoWatch
        syncInterval = directory.syncInterval
    }
}