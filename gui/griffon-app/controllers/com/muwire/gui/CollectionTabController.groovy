package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.collections.CollectionManager
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
            host : model.host,
            infoHash : collection.getInfoHash(),
            full : true,
            sequential : view.isSequentialCollection()
            )
        model.eventBus.publish(e)
        
        switchToDLTab()
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
                host : model.host,
                infoHash : collection.getInfoHash(),
                full : false,
                sequential : view.isSequentialItem()
                )
        model.eventBus.publish(e)
        
        switchToDLTab()
    }
    
    private void switchToDLTab() {
        application.mvcGroupManager.getGroups()['MainFrame'].view.showDownloadsWindow.call()
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
        mvcGroup.createMVCGroup("show-comment", params).destroy()
    }
    
    @ControllerAction
    void copyId() {
        CopyPasteSupport.copyToClipboard(model.host.toBase64())
    }
}