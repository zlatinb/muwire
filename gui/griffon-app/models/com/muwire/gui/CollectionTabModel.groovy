package com.muwire.gui

import com.muwire.core.EventBus
import com.muwire.core.collections.CollectionFetchedEvent
import com.muwire.core.collections.FileCollection
import com.muwire.core.collections.FileCollectionItem

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class CollectionTabModel {
    String fileName
    EventBus eventBus
    List<FileCollection> collections
    List<FileCollectionItem> items
    @Observable String comment
    UUID uuid
    
    void mvcGroupInit(Map<String,String> args) {
        eventBus.register(CollectionFetchedEvent.class, this)
    }
    
    void mvcGroupDestroy() {
        eventBus.unregister(CollectionFetchedEvent.class, this)
    }
    
    void onCollectionFetchedEvent(CollectionFetchedEvent e) {
        if (uuid != e.uuid)
            return
        runInsideUIAsync {
            collections.add(e.collection)
            // TODO: refresh tables
        }
    }
}