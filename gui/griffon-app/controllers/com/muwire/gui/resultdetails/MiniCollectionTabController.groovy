package com.muwire.gui.resultdetails

import com.muwire.core.collections.FileCollection
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonController)
class MiniCollectionTabController {
    
    @MVCMember @Nonnull
    MiniCollectionTabView view
    @MVCMember @Nonnull
    MiniCollectionTabModel model
    
    @ControllerAction
    void fetchCollections() {
        view.switchToTable()
        model.register()
    }
    
    @ControllerAction
    void viewComment() {
        List<FileCollection> selected = view.selectedCollections()
        if (selected.size() != 1)
            return
        String comment = selected[0].comment
        def params = [:]
        params['text'] = comment
        params['name'] = trans("COLLECTION_COMMENT")
        mvcGroup.createMVCGroup("show-comment", params).destroy()
    }
    
    @ControllerAction
    void viewCollections() {
        List<FileCollection> selected = view.selectedCollections()
        if (selected.isEmpty())
            return
        UUID uuid = UUID.randomUUID()
        def params = [:]
        params['fileName'] = selected.first().name
        params['eventBus'] = model.core.eventBus
        params['preFetchedCollections'] = selected
        params['host'] = model.resultEvent.sender
        params['uuid'] = uuid
        mvcGroup.createMVCGroup("collection-tab", uuid.toString(), params)
    }
}
