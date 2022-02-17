package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.SharedFile
import griffon.core.artifact.GriffonModel
import griffon.metadata.ArtifactProviderFor

import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

@ArtifactProviderFor(GriffonModel)
class PublishPreviewModel {
    
    
    Core core
    List<SharedFile> requested
    
    SharedFile[] toPublish, alreadyPublished
    
    void mvcGroupInit(Map<String,String> attributes) {
        Stream<SharedFile> stream = requested.stream()
        if (requested.size() > 1000)
            stream = stream.parallel()
        def map = stream.
                collect(Collectors.partitioningBy({it.isPublished()} as Predicate))
        toPublish = map.get(false).toArray(new SharedFile[0])
        alreadyPublished = map.get(true).toArray(new SharedFile[0])
    }
}
