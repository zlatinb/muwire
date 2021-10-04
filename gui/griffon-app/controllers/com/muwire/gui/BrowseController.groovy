package com.muwire.gui

import com.muwire.core.SplitPattern
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
        model.uuid = UUID.randomUUID()
        timer.start()
        core.eventBus.publish(new UIBrowseEvent(host : model.host, uuid: model.uuid))
    }
    
    void mvcGroupDestroy() {
        timer.stop()
        core.eventBus.unregister(BrowseStatusEvent.class, this)
        core.eventBus.unregister(UIResultBatchEvent.class, this)
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
            model.chatActionEnabled = event.results[0].chat
            List<UIResultEvent> results = event.results.toList()
            model.results.addAll(results)
            synchronized (model.allResults) {
                model.allResults.addAll(results)
            }
            model.resultCount = model.results.size()
        }
        if (!copy.isEmpty())
            view.refreshResults()
        
        List<BrowseStatusEvent> statusCopy
        synchronized (model.pendingStatuses) {
            statusCopy = new ArrayList<>(model.pendingStatuses)
            model.pendingStatuses.clear()
        }
        for(BrowseStatusEvent event : statusCopy) {
            model.status = event.status
            model.filterEnabled = (event.status == BrowseStatus.FINISHED || event.status == BrowseStatus.FAILED) &&
                    model.resultCount > 0
            if (event.status == BrowseStatus.FETCHING)
                model.totalResults = event.totalResults
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
        
        selectedResults.each { result ->
            def file = new File(application.context.get("muwire-settings").downloadLocation, result.name)
            core.eventBus.publish(new UIDownloadEvent(
                result : [result],
                sources : [model.host.destination],
                target : file,
                sequential : view.sequentialDownloadCheckbox.model.isSelected()
                ))
        }
        
        group.view.showDownloadsWindow.call()
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
        params['host'] = result.getSender()
        params['infoHash'] = result.getInfohash()
        params['name'] = result.getName()
        params['core'] = core
        mvcGroup.createMVCGroup("fetch-certificates", params)
    }
    
    @ControllerAction
    void chat() {
        def mainFrameGroup = application.mvcGroupManager.getGroups()['MainFrame']
        
        mainFrameGroup.controller.startChat(model.host)
        mainFrameGroup.view.showChatWindow.call()
    }
    
    @ControllerAction
    void viewCollections() {
        def selectedResults = view.selectedResults()
        if (selectedResults == null || selectedResults.size() != 1)
            return
        def event = selectedResults[0]
        if (event.collections == null || event.collections.isEmpty())
            return
        
        UUID uuid = UUID.randomUUID()
        def params = [:]
        params['fileName'] = event.name
        params['eventBus'] = mvcGroup.parentGroup.model.core.eventBus
        params['infoHashes'] = event.collections.collect()
        params['uuid'] = uuid
        params['host'] = event.sender
        mvcGroup.parentGroup.createMVCGroup("collection-tab", uuid.toString(), params)
    }
}