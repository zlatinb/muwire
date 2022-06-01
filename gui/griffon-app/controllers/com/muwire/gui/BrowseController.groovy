package com.muwire.gui

import com.muwire.core.Persona
import com.muwire.core.SplitPattern
import com.muwire.core.download.DownloadStartedEvent
import com.muwire.core.profile.MWProfileHeader
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.ResultPOP
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull

import com.muwire.core.Core
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.search.BrowseStatus
import com.muwire.core.search.BrowseStatusEvent
import com.muwire.core.search.UIBrowseEvent
import com.muwire.core.search.UIResultBatchEvent
import com.muwire.core.search.UIResultEvent

import javax.swing.JTextField

@ArtifactProviderFor(GriffonController)
class BrowseController {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    BrowseModel model
    @MVCMember @Nonnull
    BrowseView view

    Core core
    
    javax.swing.Timer timer = new javax.swing.Timer(1000, {displayBatchedResults()})
    
    void register() {
        core.eventBus.register(BrowseStatusEvent.class, this)
        core.eventBus.register(UIResultBatchEvent.class, this)
        core.eventBus.register(DownloadStartedEvent.class, this)
        model.uuid = UUID.randomUUID()
        timer.start()
        core.eventBus.publish(new UIBrowseEvent(host : model.host, uuid: model.uuid))
    }
    
    void mvcGroupDestroy() {
        timer.stop()
        core.eventBus.unregister(BrowseStatusEvent.class, this)
        core.eventBus.unregister(UIResultBatchEvent.class, this)
        core.eventBus.unregister(DownloadStartedEvent.class, this)
    }
    
    void onDownloadStartedEvent(DownloadStartedEvent event) {
        runInsideUIAsync {
            view.updateUIs()
        }
    }
    
    void onBrowseStatusEvent(BrowseStatusEvent e) {
        if (e.uuid != model.uuid)
            return
        model.pendingStatuses.add(e)
    }
    
    void onUIResultBatchEvent(UIResultBatchEvent e) {
        if (e.uuid != model.uuid)
            return
        model.pendingResults.add(e)
    }
    
    private void displayBatchedResults() {
        List<UIResultBatchEvent> copy
        synchronized(model.pendingResults) {
            copy = new ArrayList<>(model.pendingResults)
            model.pendingResults.clear()
        }
        for(UIResultBatchEvent event : copy) {
            List<UIResultEvent> results = event.results.toList()
            model.results.addAll(results)
            for (UIResultEvent result : results)
                model.resultsTreeModel.addToTree(result)
            synchronized (model.allResults) {
                model.allResults.addAll(results)
            }
            model.resultCount = model.results.size()
        }
        if (model.visible) {
            if (!copy.isEmpty() || model.dirty) {
                view.refreshResults()
                model.dirty = false
            }
        } else if (!copy.isEmpty())
            model.dirty = true
        
        List<BrowseStatusEvent> statusCopy
        synchronized (model.pendingStatuses) {
            statusCopy = new ArrayList<>(model.pendingStatuses)
            model.pendingStatuses.clear()
        }
        for(BrowseStatusEvent event : statusCopy) {
            model.status = event.status
            if (event.status == BrowseStatus.FETCHING)
                model.totalResults = event.totalResults
        }
        if (!statusCopy.isEmpty()) {
            if ((model.status == BrowseStatus.FINISHED || model.status == BrowseStatus.FAILED) &&
                    model.resultCount > 0) {
                model.filterEnabled = true
                model.cacheTopTreeLevel()
            }
        }
    }
    
    @ControllerAction
    void filter() {
        JTextField field = builder.getVariable("filter-field")
        String filter = field.getText()
        if (filter == null)
            return
        filter = filter.strip().replaceAll(SplitPattern.SPLIT_PATTERN," ").toLowerCase()
        String [] split = filter.split(" ")
        def hs = new HashSet()
        split.each {if (it.length() > 0) hs << it}
        model.filter = hs.toArray(new String[0])
        model.filterResults()
    }
    
    @ControllerAction
    void clearFilter() {
        model.filter = null
        model.clearFilterEnabled = false
        model.filterResults()
    }
    
    @ControllerAction
    void download() {
        def selectedResults = view.selectedResults()
        if (selectedResults == null || selectedResults.isEmpty())
            return
            
        def group = application.mvcGroupManager.getGroups()['MainFrame']

        selectedResults.removeAll {
            !group.model.canDownload(it.infohash)
        }
        
        File downloadsFolder = application.context.get("muwire-settings").downloadLocation
        List<ResultAndTargets> targets = view.decorateResults(selectedResults)
        targets.each { target ->
            def file = new File(downloadsFolder,target.target.toString())
            File parent = null
            if (target.parent != null)
                parent = new File(downloadsFolder, target.parent.toString())
            core.eventBus.publish(new UIDownloadEvent(
                result : [target.resultEvent],
                sources : [model.host.destination],
                target : file, 
                toShare: parent,
                sequential : view.sequentialDownloadCheckbox.model.isSelected()
                ))
        }
        
        group.view.showDownloadsWindow.call()
    }
    
    @ControllerAction
    void copyId() {
        CopyPasteSupport.copyToClipboard(model.host.toBase64())
    }
    
    @ControllerAction
    void viewDetails() {
        def selectedResults = view.selectedResults()
        if (selectedResults == null || selectedResults.size() != 1)
            return
        def event = selectedResults[0]
        List<PersonaOrProfile> senders = new ArrayList<>()
        senders << new ResultPOP(event)
        
        String mvcId = "resultdetails_" + model.host.getHumanReadableName() + "_" + Base64.encode(event.infohash.getRoot())
        def params = [:]
        params.core = core
        params.resultEvent = event
        params.senders = senders
        mvcGroup.createMVCGroup("result-details-frame", mvcId, params)
    }
    
    @ControllerAction
    void viewProfile() {
        MWProfileHeader header = null
        if (!model.allResults.isEmpty()) {
            header = model.allResults[0].profileHeader
        }
        
        UUID uuid = UUID.randomUUID()
        
        def params = [:]
        params.core = core
        params.persona = model.host
        params.uuid = uuid
        params.profileTitle = HTMLSanitizer.sanitize(header?.getTitle())
        
        mvcGroup.createMVCGroup("view-profile", uuid.toString(), params)
    }
}