package com.muwire.gui

import com.muwire.core.SharedFile

import javax.annotation.Nonnull

import com.muwire.core.Core

import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class MyFeedModel {
    
    @MVCMember @Nonnull
    MyFeedView view
    
    List<SharedFile> items = []
    
    Core core
    
    @Observable boolean unpublishActionEnabled
    @Observable int itemsCount
 
    void mvcGroupInit(Map<String,String> map) {
        items.addAll(core.getFileManager().getPublishedSince(0))
        itemsCount = items.size()
    }
}