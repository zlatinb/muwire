package com.muwire.gui.resultdetails

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile
import com.muwire.core.search.UIResultEvent
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import griffon.transform.Observable
import net.i2p.data.Base64

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonModel)
class ResultDetailsTabsModel {
    
    @MVCMember @Nonnull
    ResultDetailsTabsView view
    
    @Observable boolean browseActionEnabled
    @Observable boolean copyIdActionEnabled

    Core core
    String fileName
    InfoHash infoHash
    List<UIResultEvent> results
    String uuid

    String key
    
    private final Set<UIResultEvent> uniqueResults = new HashSet<>()
    List<UIResultEvent> resultsWithComments = []
    List<UIResultEvent> resultsWithCertificates = []
    List<UIResultEvent> resultsWithCollections = []
    
    void mvcGroupInit(Map<String,String> args) {
        key = fileName + Base64.encode(infoHash.getRoot())
        
        uniqueResults.addAll(results)
        for (UIResultEvent event : results) {
            if (event.comment != null)
                resultsWithComments << event
            if (event.certificates > 0)
                resultsWithCertificates << event
            if (event.collections.size() > 0)
                resultsWithCollections << event
        }
    }
    
    List<String> getLocalCopies() {
        SharedFile[] sharedFiles = core.fileManager.getSharedFiles(infoHash.getRoot())
        if (sharedFiles == null || sharedFiles.length == 0)
            return null
        sharedFiles.collect {it.getCachedPath()}
    }
    
    void addResult(UIResultEvent event) {
        if (!uniqueResults.add(event))
            return
        results << event
        if (event.comment != null)
            resultsWithComments << event
        if (event.certificates > 0)
            resultsWithCertificates << event
        if (event.collections.size() > 0)
            resultsWithCollections << event
        view.addResultToListGroups(event)
        view.refreshAll()
    }
}
