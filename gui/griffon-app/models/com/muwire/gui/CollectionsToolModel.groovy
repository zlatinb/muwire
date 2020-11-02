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
    FileCollection collection 
    List<FileCollection.SearchHit> hits = new ArrayList<>()
    
    void mvcGroupInit(Map<String,String> args) {
        hits.addAll(collection.hits)
    }  
}