package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.collections.FileCollectionItem

@ArtifactProviderFor(GriffonController)
class CollectionTabController {
    @MVCMember @Nonnull
    CollectionTabModel model
    @MVCMember @Nonnull
    CollectionTabView view

    @ControllerAction
    void downloadCollection() {
        
    }
    
    @ControllerAction
    void download() {
        
    }
    
    @ControllerAction
    void viewComment() {
        List<FileCollectionItem> items = view.selectedItems()
        if (items.size() != 1)
            return
            
        def item = items.get(0)
        String text = item.comment
        String name = String.join(File.separator, item.pathElements)
        
        def params = [:]
        params['text'] = text
        params['name'] = name
        mvcGroup.createMVCGroup("show-comment", params)
    }
}