package com.muwire.gui

import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.core.mvc.MVCGroup
import griffon.core.mvc.MVCGroupConfiguration
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull
import javax.inject.Inject

import com.muwire.core.Core
import com.muwire.core.download.DownloadStartedEvent
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

@ArtifactProviderFor(GriffonController)
class MainFrameController {
    @Inject @Nonnull GriffonApplication application
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    
    @MVCMember @Nonnull
    MainFrameModel model

    private volatile Core core
    
    @ControllerAction
    void search() {
        def search = builder.getVariable("search-field").text
        def uuid = UUID.randomUUID()
        Map<String, Object> params = new HashMap<>()
        params["search-terms"] = search
        params["uuid"] = uuid.toString()
        def group = mvcGroup.createMVCGroup("SearchTab", uuid.toString(), params)
        model.results[uuid.toString()] = group
        
        def searchEvent = new SearchEvent(searchTerms : [search], uuid : uuid)
        core.eventBus.publish(new QueryEvent(searchEvent : searchEvent, firstHop : true, 
            replyTo: core.me.destination, receivedOn: core.me.destination))
    }
    
    private def selectedResult() {
        def selected = builder.getVariable("result-tabs").getSelectedComponent()
        def group = selected.getClientProperty("mvc-group")
        def table = selected.getClientProperty("results-table")
        int row = table.getSelectedRow()
        if (row == -1)
            return
        group.model.results[row]        
    }
    
    private def selectedDownload() {
        def selected = builder.getVariable("downloads-table").getSelectedRow()
        model.downloads[selected].downloader
    }
    
    @ControllerAction
    void download() {
        def result = selectedResult()
        if (result == null)
            return // TODO disable button
        def file = new File(application.context.get("muwire-settings").downloadLocation, result.name) 
        core.eventBus.publish(new UIDownloadEvent(result : result, target : file))
    }
    
    @ControllerAction
    void trust() {
        def result = selectedResult()
        if (result == null)
            return // TODO disable button
        core.eventBus.publish( new TrustEvent(destination : result.sender.destination, level : TrustLevel.TRUSTED))
    }
    
    @ControllerAction
    void distrust() {
        def result = selectedResult()
        if (result == null)
            return // TODO disable button
        core.eventBus.publish( new TrustEvent(destination : result.sender.destination, level : TrustLevel.DISTRUSTED))
    }
    
    @ControllerAction 
    void cancel() {
        def downloader = selectedDownload()
        downloader.cancel()
    }
    
    void mvcGroupInit(Map<String, String> args) {
        application.addPropertyChangeListener("core", {e-> 
            core = e.getNewValue()
        })
    }
}