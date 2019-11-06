package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull

import com.muwire.core.Core
import com.muwire.core.EventBus
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.search.BrowseStatus
import com.muwire.core.search.BrowseStatusEvent
import com.muwire.core.search.UIBrowseEvent
import com.muwire.core.search.UIResultEvent

@ArtifactProviderFor(GriffonController)
class BrowseController {
    @MVCMember @Nonnull
    BrowseModel model
    @MVCMember @Nonnull
    BrowseView view

    Core core
    
    
    void register() {
        core.eventBus.register(BrowseStatusEvent.class, this)
        core.eventBus.register(UIResultEvent.class, this)
        core.eventBus.publish(new UIBrowseEvent(host : model.host))
    }
    
    void mvcGroupDestroy() {
        core.eventBus.unregister(BrowseStatusEvent.class, this)
        core.eventBus.unregister(UIResultEvent.class, this)
    }
    
    void onBrowseStatusEvent(BrowseStatusEvent e) {
        runInsideUIAsync {
            model.status = e.status
            if (e.status == BrowseStatus.FETCHING)
                model.totalResults = e.totalResults
        }
    }
    
    void onUIResultEvent(UIResultEvent e) {
        runInsideUIAsync {
            model.results << e
            model.resultCount = model.results.size()
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
        def selectedResults = view.selectedResults()
        if (selectedResults == null || selectedResults.isEmpty())
            return
        selectedResults.removeAll {
            !mvcGroup.parentGroup.parentGroup.model.canDownload(it.infohash)
        }
        
        selectedResults.each { result ->
            def file = new File(application.context.get("muwire-settings").downloadLocation, result.name)
            core.eventBus.publish(new UIDownloadEvent(
                result : [result],
                sources : [model.host.destination],
                target : file,
                sequential : mvcGroup.parentGroup.view.sequentialDownloadCheckbox.model.isSelected()
                ))
        }
        
        mvcGroup.parentGroup.parentGroup.view.showDownloadsWindow.call()
        dismiss()
    }
    
    @ControllerAction
    void viewComment() {
        def selectedResults = view.selectedResults()
        if (selectedResults == null || selectedResults.size() != 1)
            return
        def result = selectedResults[0]
        if (result.comment == null)
            return
        
        String groupId = Base64.encode(result.infohash.getRoot())
        Map<String,Object> params = new HashMap<>()
        params['text'] = result.comment
        params['name'] = result.name
        
        mvcGroup.createMVCGroup("show-comment", groupId, params)
    }
    
    @ControllerAction
    void viewCertificates() {
        def selectedResults = view.selectedResults()
        if (selectedResults == null || selectedResults.size() != 1)
            return
        def result = selectedResults[0]
        if (result.certificates <= 0)
            return
        
        def params = [:]
        params['result'] = result
        params['core'] = core
        mvcGroup.createMVCGroup("fetch-certificates", params)
    }
}