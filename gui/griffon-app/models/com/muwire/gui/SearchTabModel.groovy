package com.muwire.gui

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JTable

import com.muwire.core.Core
import com.muwire.core.search.UIResultEvent

import griffon.core.artifact.GriffonModel
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class SearchTabModel {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    
    Core core
    String uuid
    def results = []
    def hashCount = [:]
    
    
    void mvcGroupInit(Map<String, String> args) {
        core = mvcGroup.parentGroup.model.core
        mvcGroup.parentGroup.model.results[UUID.fromString(uuid)] = mvcGroup
    }
    
    void mvcGroupDestroy() {
        mvcGroup.parentGroup.model.results.remove(uuid)
    }
    
    void handleResult(UIResultEvent e) {
        runInsideUIAsync {
            Integer count = hashCount.get(e.infohash)
            if (count == null) 
                count = 0
            count++
            hashCount[e.infohash] = count
                            
            results << e
            JTable table = builder.getVariable("results-table")
            table.model.fireTableDataChanged()
        }
    }
}