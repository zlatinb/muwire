package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.filefeeds.Feed

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class FeedConfigurationModel {
    Core core
    Feed feed
    
    @Observable boolean autoDownload
    @Observable boolean sequential
    @Observable int updateInterval
    @Observable int itemsToKeep
    
    void mvcGroupInit(Map<String, String> args) {
        autoDownload = feed.isAutoDownload()
        sequential = feed.isSequential()
        updateInterval = feed.getUpdateInterval() / 60000
        itemsToKeep = feed.getItemsToKeep()
    }
}