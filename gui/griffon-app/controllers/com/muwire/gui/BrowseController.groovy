package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.EventBus
import com.muwire.core.search.BrowseStatusEvent
import com.muwire.core.search.UIBrowseEvent
import com.muwire.core.search.UIResultEvent

@ArtifactProviderFor(GriffonController)
class BrowseController {
    @MVCMember @Nonnull
    BrowseModel model
    @MVCMember @Nonnull
    BrowseView view

    EventBus eventBus
    
    
    void register() {
        eventBus.register(BrowseStatusEvent.class, this)
        eventBus.register(UIResultEvent.class, this)
        eventBus.publish(new UIBrowseEvent(host : model.host))
    }
    
    void mvcGroupDestroy() {
        eventBus.unregister(BrowseStatusEvent.class, this)
        eventBus.unregister(UIResultEvent.class, this)
    }
    
    void onBrowseStatusEvent(BrowseStatusEvent e) {
        runInsideUIAsync {
            model.status = e.status
        }
    }
    
    void onUIResultEvent(UIResultEvent e) {
        runInsideUIAsync {
            model.results << e
            view.resultsTable.model.fireTableDataChanged()
        }
    }
    
    @ControllerAction
    void dismiss() {
        view.dialog.setVisible(false)
        mvcGroup.destroy()
    }
    
    @ControllerAction
    void download() {
        
    }
    
    @ControllerAction
    void viewComment() {
        
    }
}