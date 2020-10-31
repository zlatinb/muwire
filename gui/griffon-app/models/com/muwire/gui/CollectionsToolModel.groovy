package com.muwire.gui

import com.muwire.core.EventBus
import com.muwire.core.SharedFile
import com.muwire.core.collections.CollectionManager
import com.muwire.core.collections.FileCollection
import com.muwire.core.files.FileManager

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class CollectionsToolModel {
    
    FileManager fileManager
    CollectionManager collectionManager
    EventBus eventBus
    
    @Observable boolean viewCommentButtonEnabled
    @Observable boolean viewFileCommentButtonEnabled
    @Observable boolean deleteButtonEnabled
    
    List<FileCollection> collections = new ArrayList<>()
    List<SharedFile> files = new ArrayList<>()
    // TODO: add tree view
    
    void mvcGroupInit(Map<String,String> args) {
        collections.addAll(collectionManager.getCollections())
    }
}