package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull
import javax.swing.JOptionPane

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.filefeeds.Feed
import com.muwire.core.filefeeds.UIFeedConfigurationEvent
import com.muwire.core.search.UIResultEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

@ArtifactProviderFor(GriffonController)
class SearchTabController {

    @MVCMember @Nonnull
    SearchTabModel model
    @MVCMember @Nonnull
    SearchTabView view

    Core core

    private def selectedResults() {
        if (model.groupedByFile) {
             return [view.getSelectedResult()]   
        } else {
            int[] rows = view.resultsTable.getSelectedRows()
            if (rows.length == 0)
                return null
            def sortEvt = view.lastSortEvent
            if (sortEvt != null) {
                for (int i = 0; i < rows.length; i++) {
                    rows[i] = view.resultsTable.rowSorter.convertRowIndexToModel(rows[i])
                }
            }
            List<UIResultEvent> results = new ArrayList<>()
            rows.each { results.add(model.results[it]) }
            return results
        }
    }

    @ControllerAction
    void download() {
        def results = selectedResults()
        if (results == null)
            return

        results.removeAll {
            !mvcGroup.parentGroup.model.canDownload(it.infohash)
        }

        results.each { result ->
            def file = new File(application.context.get("muwire-settings").downloadLocation, result.name)

            def resultsBucket = model.hashBucket[result.infohash]
            def sources = model.sourcesBucket[result.infohash]

            core.eventBus.publish(new UIDownloadEvent(result : resultsBucket, sources: sources,
            target : file, sequential : view.sequentialDownload()))
        }
        mvcGroup.parentGroup.view.showDownloadsWindow.call()
    }

    @ControllerAction
    void trust() {
        def sender = view.selectedSender()
        if (sender == null)
            return
        String reason = JOptionPane.showInputDialog("Enter reason (optional)")
        core.eventBus.publish( new TrustEvent(persona : sender, level : TrustLevel.TRUSTED, reason : reason))
    }

    @ControllerAction
    void distrust() {
        def sender = view.selectedSender()
        if (sender == null)
            return
        String reason = JOptionPane.showInputDialog("Enter reason (optional)")
        core.eventBus.publish( new TrustEvent(persona : sender, level : TrustLevel.DISTRUSTED, reason : reason))
    }

    @ControllerAction
    void neutral() {
        def sender = view.selectedSender()
        if (sender == null)
            return
        core.eventBus.publish( new TrustEvent(persona : sender, level : TrustLevel.NEUTRAL))
    }

    @ControllerAction
    void browse() {
        def sender = view.selectedSender()
        if (sender == null)
            return

        String groupId = sender.getHumanReadableName() + "-browse"
        Map<String,Object> params = new HashMap<>()
        params['host'] = sender
        params['core'] = core

        mvcGroup.createMVCGroup("browse", groupId, params)
    }
    
    @ControllerAction
    void subscribe() {
        def sender = view.selectedSender()
        if (sender == null)
            return

        Feed feed = new Feed(sender)
        feed.setAutoDownload(core.muOptions.defaultFeedAutoDownload)
        feed.setSequential(core.muOptions.defaultFeedSequential)
        feed.setItemsToKeep(core.muOptions.defaultFeedItemsToKeep)
        feed.setUpdateInterval(core.muOptions.defaultFeedUpdateInterval)
        
        core.eventBus.publish(new UIFeedConfigurationEvent(feed : feed, newFeed: true))  
        mvcGroup.parentGroup.view.showFeedsWindow.call()          
    }
    
    @ControllerAction
    void chat() {
        def sender = view.selectedSender()
        if (sender == null)
            return
        
        def parent = mvcGroup.parentGroup
        parent.controller.startChat(sender)
        parent.view.showChatWindow.call()
    }

    @ControllerAction
    void showComment() {
        UIResultEvent event = view.getSelectedResult()
        if (event == null || event.comment == null)
            return

        String groupId = Base64.encode(event.infohash.getRoot())
        Map<String,Object> params = new HashMap<>()
        params['text'] = event.comment
        params['name'] = event.name

        mvcGroup.createMVCGroup("show-comment", groupId, params)
    }

    @ControllerAction
    void viewCertificates() {
        UIResultEvent event = view.getSelectedResult()
        if (event == null || event.certificates <= 0)
            return

        def params = [:]
        params['host'] = event.getSender()
        params['infoHash'] = event.getInfohash()
        params['name'] = event.getName()
        params['core'] = core
        mvcGroup.createMVCGroup("fetch-certificates", params)
    }
    
    @ControllerAction
    void viewCollections() {
        UIResultEvent event = view.getSelectedResult()
        if (event == null || event.collections.isEmpty())
            return
            
        def params = [:]
        params['fileName'] = event.name
        params['eventBus'] = mvcGroup.parentGroup.model.core.eventBus
        params['collections'] = event.collections.collect()
        params['uuid'] = UUID.randomUUID()
        mvcGroup.createMVCGroup("collection-tab", params)
    }
}