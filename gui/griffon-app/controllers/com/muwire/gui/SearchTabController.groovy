package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.Core
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

@ArtifactProviderFor(GriffonController)
class SearchTabController {
    
    @MVCMember @Nonnull
    SearchTabModel model
    @MVCMember @Nonnull
    SearchTabView view
    
    Core core
    
    private def selectedResult() {
        int row = view.resultsTable.getSelectedRow()
        if (row == -1)
            return null
        def sortEvt = view.lastSortEvent
        if (sortEvt != null) {
            row = view.resultsTable.rowSorter.convertRowIndexToModel(row)
        }
        model.results[row]
    }
    
    @ControllerAction
    void download() {
        def result = selectedResult()
        if (result == null)
            return

        if (!mvcGroup.parentGroup.model.canDownload(result.infohash))
            return

        def file = new File(application.context.get("muwire-settings").downloadLocation, result.name)

        def resultsBucket = model.hashBucket[result.infohash]
        def sources = model.sourcesBucket[result.infohash]

        core.eventBus.publish(new UIDownloadEvent(result : resultsBucket, sources: sources, target : file))
        mvcGroup.parentGroup.view.showDownloadsWindow.call()
    }

    @ControllerAction
    void trust() {
        def result = selectedResult()
        if (result == null)
            return 
        core.eventBus.publish( new TrustEvent(persona : result.sender, level : TrustLevel.TRUSTED))
    }

    @ControllerAction
    void distrust() {
        def result = selectedResult()
        if (result == null)
            return 
        core.eventBus.publish( new TrustEvent(persona : result.sender, level : TrustLevel.DISTRUSTED))
    }
}