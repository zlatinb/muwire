package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.InfoHash
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
    
    @Observable boolean showCommentActionEnabled
    
    public void mvcGroupInit(Map<String,String> args) {
        searchers.addAll(sf.getSearches())
        downloaders.addAll(sf.getDownloaders())
        certificates.addAll(core.certificateManager.byInfoHash.getOrDefault(new InfoHash(sf.getRoot()),[]))
    }
}