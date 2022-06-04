package com.muwire.gui.resultdetails

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile
import com.muwire.core.search.UIResultEvent
import com.muwire.gui.profile.ResultPOP
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
    @Observable boolean viewProfileActionEnabled

    Core core
    String fileName
    InfoHash infoHash
    List<ResultPOP> results
    String uuid

    String key
    
    private final Set<UIResultEvent> uniqueResults = new HashSet<>()
    List<ResultPOP> resultsWithComments = []
    List<ResultPOP> resultsWithCertificates = []
    List<ResultPOP> resultsWithCollections = []
    
    void mvcGroupInit(Map<String,String> args) {
        key = fileName + Base64.encode(infoHash.getRoot())
        
        uniqueResults.addAll(results.collect {it.getEvent()})
        for (ResultPOP resultPOP : results) {
            def event = resultPOP.getEvent()
            if (event.comment != null)
                resultsWithComments << resultPOP
            if (event.certificates > 0)
                resultsWithCertificates << resultPOP
            if (event.collections.size() > 0)
                resultsWithCollections << resultPOP
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
        def resultPOP = new ResultPOP(event)
        results << resultPOP
        if (event.comment != null)
            resultsWithComments << resultPOP
        if (event.certificates > 0)
            resultsWithCertificates << resultPOP
        if (event.collections.size() > 0)
            resultsWithCollections << resultPOP
        view.addResultToListGroups(resultPOP)
        view.refreshAll()
    }
}
