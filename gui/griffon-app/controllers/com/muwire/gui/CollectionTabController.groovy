package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class CollectionTabController {
    @MVCMember @Nonnull
    CollectionTabModel model
    @MVCMember @Nonnull
    CollectionTabView view

    @ControllerAction
    void download() {
        
    }
    
    @ControllerAction
    void viewComment() {
        int []rows = view.selectedItems()
        if (rows.length != 1)
            return
            
        def item = model.items.get(rows[0])
        String text = item.comment
        String name = String.join(File.separator, item.pathElements)
        
        def params = [:]
        params['text'] = text
        params['name'] = name
        mvcGroup.createMVCGroup("show-comment", params)
    }
}