package com.muwire.gui

import com.muwire.core.content.ContentManager

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class ContentPanelModel {
    
    private ContentManager contentManager
    
    def rules = []
    def hits = []
    
    @Observable boolean regex
    
    void mvcGroupInit(Map<String,String> args) {
        contentManager = application.context.get("core").contentManager
        refresh()
    }
    
    void refresh() {
        rules.clear()
        rules.addAll(contentManager.matchers)
        hits.clear()
        // TODO: fire table data changed event
    }
}