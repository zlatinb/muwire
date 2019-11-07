package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.SharedFile

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class SharedFileModel {
    SharedFile sf
    Core core
    
    def searchers = []
    def downloaders = []
    def certificates = []
    
    public void mvcGroupInit(Map<String,String> args) {
        searchers.addAll(sf.searches)
        downloaders.addAll(sf.downloaders)
        certificates.addAll(core.certificateManager.byInfoHash.get(sf.infoHash))
    }
}