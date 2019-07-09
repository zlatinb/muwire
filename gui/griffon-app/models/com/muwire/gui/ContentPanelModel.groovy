package com.muwire.gui

import javax.annotation.Nonnull

import com.muwire.core.Core
import com.muwire.core.EventBus
import com.muwire.core.content.ContentControlEvent
import com.muwire.core.content.ContentManager

import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class ContentPanelModel {
    
    @MVCMember @Nonnull
    ContentPanelView view
    
    Core core
    
    private ContentManager contentManager
    
    def rules = []
    def hits = []
    
    @Observable boolean regex
    @Observable boolean deleteButtonEnabled
    @Observable boolean trustButtonsEnabled
    
    void mvcGroupInit(Map<String,String> args) {
        contentManager = application.context.get("core").contentManager
        rules.addAll(contentManager.matchers)
        core.eventBus.register(ContentControlEvent.class, this)
    }
    
    void mvcGroupDestroy() {
        core.eventBus.unregister(ContentControlEvent.class, this)
    }
    
    void refresh() {
        rules.clear()
        rules.addAll(contentManager.matchers)
        hits.clear()
        view.rulesTable.model.fireTableDataChanged()
    }
    
    void onContentControlEvent(ContentControlEvent e) {
        runInsideUIAsync {
            refresh()
        }
    }
}