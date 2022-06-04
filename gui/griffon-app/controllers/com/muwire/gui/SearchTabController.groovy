package com.muwire.gui

import com.muwire.core.InfoHash
import com.muwire.core.SplitPattern
import com.muwire.core.download.DownloadStartedEvent
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.ViewProfileHelper
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JOptionPane

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.filefeeds.Feed
import com.muwire.core.filefeeds.UIFeedConfigurationEvent
import com.muwire.core.search.UIResultEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

import javax.swing.JTextField
import java.util.stream.Collectors

@ArtifactProviderFor(GriffonController)
class SearchTabController {

    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    SearchTabModel model
    @MVCMember @Nonnull
    SearchTabView view
    @Inject
    GriffonApplication application

    Core core

    void mvcGroupInit(Map<String,String> args) {
        core.eventBus.register(DownloadStartedEvent.class, this)
    }
    
    void mvcGroupDestroy() {
        core.eventBus.unregister(DownloadStartedEvent.class, this)
    }
    
    void onDownloadStartedEvent(DownloadStartedEvent event) {
        runInsideUIAsync {
            view.updateUIs()
        }
    }
    
    @ControllerAction
    void download() {
        def results = view.selectedResults()
        if (results.isEmpty())
            return

        results.removeAll {
            !mvcGroup.parentGroup.model.canDownload(it.infohash)
        }
        
        Set<InfoHash> uniqueHashes = results.stream().map({it.infohash}).collect(Collectors.toSet())
        results = uniqueHashes.stream().map({model.hashBucket[it].firstEvent()}).collect(Collectors.toList())

        File downloadsFolder = application.context.get("muwire-settings").downloadLocation
        List<ResultAndTargets> targets = view.decorateResults(results)
        
        targets.each { target ->
            File file = new File(downloadsFolder, target.target.toString())
            File parent = null
            if (target.parent != null)
                parent = new File(downloadsFolder, target.parent.toString())

            def resultsBucket = model.hashBucket[target.resultEvent.infohash].getResults()
            def sources = model.sourcesBucket[target.resultEvent.infohash]

            core.eventBus.publish(new UIDownloadEvent(result : resultsBucket, sources: sources,
            target : file, toShare: parent, sequential : view.sequentialDownload()))
        }
        mvcGroup.parentGroup.view.showDownloadsWindow.call()
    }

    @ControllerAction
    void viewProfile() {
        def sender = view.selectedSender()
        if (sender == null)
            return
        
        ViewProfileHelper.initViewProfileGroup(model.core, mvcGroup, sender)
    }

    @ControllerAction
    void browse() {
        def sender = view.selectedSender()
        if (sender == null)
            return

        sender = sender.getPersona()
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
        
        sender = sender.getPersona()
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

        sender = sender.getPersona()
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
        
        sender = sender.getPersona()
        def parent = mvcGroup.parentGroup
        parent.controller.startChat(sender)
        parent.view.showChatWindow.call()
    }

    @ControllerAction
    void message() {
        Persona recipient = view.selectedSender()?.getPersona()
        
        if (recipient == null)
            return
        
        def params = [:]
        params.recipients = new HashSet<>(Collections.singletonList(recipient))
        params.core = model.core
        mvcGroup.parentGroup.createMVCGroup("new-message", UUID.randomUUID().toString(), params)
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
        model.filterResults2()
    }
    
    @ControllerAction
    void clearFilter() {
        model.clearFilterActionEnabled = false
        model.filter = null
        model.filterResults2()
        view.clearSelections()
    }
    
    @ControllerAction
    void viewDetails() {
        def events = view.selectedResults()
        if (events.size() != 1)
            return
        UIResultEvent event = events.first()
        List<PersonaOrProfile> senders = new ArrayList<>(model.hashBucket[event.infohash].getPOPs())
        
        String mvcId = "resultdetails_" + model.uuid + "_" + event.sender.getHumanReadableName() + "_" + Base64.encode(event.infohash.getRoot())
        if (application.getMvcGroupManager().findGroup(mvcId) != null)
            return
        
        def params = [:]
        params.core = model.core
        params.resultEvent = event
        params.senders = senders
        params.uuid = model.uuid
        mvcGroup.createMVCGroup("result-details-frame", mvcId, params)
    }
}