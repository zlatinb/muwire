package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.collections.FileCollection
import com.muwire.core.collections.FileCollectionItem
import com.muwire.core.collections.UIDownloadCollectionEvent

@ArtifactProviderFor(GriffonController)
class CollectionTabController {
    @MVCMember @Nonnull
    CollectionTabModel model
    @MVCMember @Nonnull
    CollectionTabView view

    private FileCollection selectedCollection() {
        int row = view.selectedCollection()
        if (row < 0)
            return null
        
        return model.collections.get(row)
    }
    
    @ControllerAction
    void downloadCollection() {
        FileCollection collection = selectedCollection()
        if (collection == null)
            return
            
        UIDownloadCollectionEvent e = new UIDownloadCollectionEvent(
            collection : collection,
            items : collection.getFiles(),
            full : true,
            host : model.host
            )
        model.eventBus.publish(e)
    }
    
    @ControllerAction
    void download() {
        FileCollection collection = selectedCollection()
        if (collection == null)
            return
        
        List<FileCollectionItem> items = view.selectedItems()
        if (items.isEmpty())
            return
            
        UIDownloadCollectionEvent e = new UIDownloadCollectionEvent(
                collection : collection,
                items : new HashSet<>(items),
                full : false,
                host : model.host
                )
        model.eventBus.publish(e)
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