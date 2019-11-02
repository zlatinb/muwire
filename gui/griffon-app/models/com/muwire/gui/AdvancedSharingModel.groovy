package com.muwire.gui

import com.muwire.core.Core

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class AdvancedSharingModel {
    def watchedDirectories = []
    
    Core core
    
    void mvcGroupInit(Map<String,String> args) {
        watchedDirectories.addAll(core.muOptions.watchedDirectories)
//        view.watchedDirsTable.model.fireTableDataChanged()
    }
    
}