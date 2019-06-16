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
    UISettings uiSettings
    String uuid
    def results = []
    def hashBucket = [:]
    
    
    void mvcGroupInit(Map<String, String> args) {
        core = mvcGroup.parentGroup.model.core
        uiSettings = application.context.get("ui-settings")
        mvcGroup.parentGroup.model.results[UUID.fromString(uuid)] = mvcGroup
    }
    
    void mvcGroupDestroy() {
        mvcGroup.parentGroup.model.results.remove(uuid)
    }
    
    void handleResult(UIResultEvent e) {
        if (uiSettings.excludeLocalResult &&
            e.sender == core.me)
            return
        runInsideUIAsync {
            def bucket = hashBucket.get(e.infohash)
            if (bucket == null) {
                bucket = []
                hashBucket[e.infohash] = bucket
            }
            bucket << e
                            
            results << e
            JTable table = builder.getVariable("results-table")
            table.model.fireTableDataChanged()
        }
    }
    
    void handleResultBatch(UIResultEvent[] batch) {
        runInsideUIAsync {
            batch.each { 
                if (uiSettings.excludeLocalResult && it.sender == core.me)
                    return
                def bucket = hashBucket.get(it.infohash)
                if (bucket == null) {
                    bucket = []
                    hashBucket[it.infohash] = bucket
                }
                bucket << it
                results << it
            }
            JTable table = builder.getVariable("results-table")
            table.model.fireTableDataChanged()
        }
    }
}