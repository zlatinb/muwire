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

    @ControllerAction
    void download() {
        def results = view.selectedResults()
        if (results == null || results.isEmpty())
            return

        results.removeAll {
            !mvcGroup.parentGroup.model.canDownload(it.infohash)
        }

        File downloadsFolder = application.context.get("muwire-settings").downloadLocation
        List<ResultAndTargets> targets = view.decorateResults(results)
        
        targets.each { target ->
            File file = new File(downloadsFolder, target.target.toString())
            File parent = null
            if (target.parent != null)
                parent = new File(downloadsFolder, target.parent.toString())

            def resultsBucket = model.hashBucket[target.resultEvent.infohash]
            def sources = model.sourcesBucket[target.resultEvent.infohash]

            core.eventBus.publish(new UIDownloadEvent(result : resultsBucket, sources: sources,
            target : file, toShare: parent, sequential : view.sequentialDownload()))
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

        String groupId = UUID.randomUUID().toString()
        Map<String,Object> params = new HashMap<>()
        params['host'] = sender
        params['core'] = core

        mvcGroup.parentGroup.createMVCGroup("browse", groupId, params)
    }
    
    @ControllerAction
    void browseCollections() {
        def sender = view.selectedSender()
        if (sender == null)
            return
        
        UUID uuid = UUID.randomUUID()
        def params = [:]
        params['fileName'] = sender.getHumanReadableName()
        params['eventBus'] = mvcGroup.parentGroup.model.core.eventBus
        params['everything'] = true 
        params['uuid'] = uuid
        params['host'] = sender
        mvcGroup.parentGroup.createMVCGroup("collection-tab", uuid.toString(), params)
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
            
        UUID uuid = UUID.randomUUID()
        def params = [:]
        params['fileName'] = event.name
        params['eventBus'] = mvcGroup.parentGroup.model.core.eventBus
        params['infoHashes'] = event.collections.collect()
        params['uuid'] = uuid
        params['host'] = event.sender
        mvcGroup.parentGroup.createMVCGroup("collection-tab", uuid.toString(), params)
    }
    
    @ControllerAction
    void message() {
        Persona recipient = view.selectedSender()
        if (recipient == null)
            return
        
        def params = [:]
        params.recipients = new HashSet<>(Collections.singletonList(recipient))
        params.core = model.core
        mvcGroup.parentGroup.createMVCGroup("new-message", UUID.randomUUID().toString(), params)
    }
    
    @ControllerAction
    void copyFullID() {
        Persona sender = view.selectedSender()
        if (sender == null)
            return
        CopyPasteSupport.copyToClipboard(sender.toBase64())
    }
}