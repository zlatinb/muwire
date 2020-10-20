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
import com.muwire.core.search.UIResultBatchEvent
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
        core.eventBus.register(UIResultBatchEvent.class, this)
        core.eventBus.publish(new UIBrowseEvent(host : model.host))
    }
    
    void mvcGroupDestroy() {
        core.eventBus.unregister(BrowseStatusEvent.class, this)
        core.eventBus.unregister(UIResultBatchEvent.class, this)
    }
    
    void onBrowseStatusEvent(BrowseStatusEvent e) {
        runInsideUIAsync {
            model.status = e.status
            if (e.status == BrowseStatus.FETCHING) {
                model.totalResults = e.totalResults
                model.uuid = e.uuid
            }
        }
    }
    
    void onUIResultBatchEvent(UIResultBatchEvent e) {
        runInsideUIAsync {
            if (e.uuid != model.uuid)
                return
            model.chatActionEnabled = e.results[0].chat
            model.results.addAll(e.results.toList())
            model.resultCount = model.results.size()
            
            int [] selectedRows = view.resultsTable.getSelectedRows()
            if (view.lastSortEvent != null) {
                for (int i = 0; i < selectedRows.length; i ++)
                    selectedRows[i] = view.resultsTable.rowSorter.convertRowIndexToModel(selectedRows[i])
            }
            view.resultsTable.model.fireTableDataChanged()
            if (view.lastSortEvent != null) {
                for (int i = 0; i < selectedRows.length; i ++)
                    selectedRows[i] = view.resultsTable.rowSorter.convertRowIndexToView(selectedRows[i])
            }
            for (int row : selectedRows) 
                view.resultsTable.selectionModel.addSelectionInterval(row, row)
            
        }
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
}