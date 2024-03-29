package com.muwire.gui

import com.muwire.core.InfoHash
import com.muwire.core.profile.MWProfileHeader
import com.muwire.core.trust.TrustEvent
import com.muwire.gui.profile.ImageScaler
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.ResultPOP
import com.muwire.gui.profile.ThumbnailIcon

import javax.annotation.Nonnull

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.search.UIResultEvent

import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.SwingWorker
import javax.swing.tree.DefaultMutableTreeNode
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@ArtifactProviderFor(GriffonModel)
class SearchTabModel {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    SearchTabView view
    
    @Observable boolean downloadActionEnabled
    @Observable boolean browseActionEnabled
    @Observable boolean browseCollectionsActionEnabled
    @Observable boolean chatActionEnabled
    @Observable boolean subscribeActionEnabled
    @Observable boolean messageActionEnabled
    @Observable boolean viewProfileActionEnabled
    @Observable boolean viewDetailsActionEnabled
    @Observable boolean groupedByFile
    

    Core core
    UISettings uiSettings
    String uuid
    Integer tab
    Boolean regex
    
    boolean visible
    List<UIResultEvent> pendingResults = Collections.synchronizedList(new ArrayList<>())
    javax.swing.Timer timer = new javax.swing.Timer(500, {displayBatchedResults()})
    
    def senders = []
    def results = []
    def hashBucket = new ConcurrentHashMap<InfoHash, HashBucket>()
    def sourcesBucket = [:]
    def sendersBucket = new LinkedHashMap<>()
    
    def results2 = []
    def allResults2 = new LinkedHashSet()
    volatile String[] filter
    volatile Filterer filterer
    @Observable boolean clearFilterActionEnabled
    

    ResultTreeModel treeModel
    DefaultMutableTreeNode root
    boolean treeVisible = true
    
    void mvcGroupInit(Map<String, String> args) {
        root = new ResultTreeModel.MutableResultNode()
        treeModel = new ResultTreeModel(root)
        core = mvcGroup.parentGroup.model.core
        uiSettings = application.context.get("ui-settings")
        
        core.getEventBus().register(TrustEvent.class, this)
        
        timer.start()
        mvcGroup.parentGroup.model.results[UUID.fromString(uuid)] = mvcGroup
    }

    void mvcGroupDestroy() {
        timer.stop()
        core.getEventBus().unregister(TrustEvent.class, this)
        mvcGroup.parentGroup.model.results.remove(uuid)
    }
    
    void onTrustEvent(TrustEvent event) {
        runInsideUIAsync {
            view.onTrustChanged(event.persona)
        }
    }
    
    private boolean filter(InfoHash infoHash) {
        if (filter == null)
            return true
        String name = hashBucket[infoHash].getName().toLowerCase()
        boolean contains = true
        for (String keyword : filter) {
            contains &= name.contains(keyword)
        }
        contains
    }
    
    void filterResults2() {
        results2.clear()
        view.refreshResults()
        filterer?.cancel()
        if (filter != null) {
            setClearFilterActionEnabled(false)
            filterer = new Filterer()
            filterer.execute()
        } else {
            synchronized (allResults2) {
                results2.addAll(allResults2)
            }
            view.refreshResults()
        }
    }

    void handleResultBatch(UIResultEvent[] batch) {
        synchronized (pendingResults) {
            for(UIResultEvent event : batch)
                pendingResults << event
        }
    }
    
    void setVisible(boolean visible) {
        boolean oldVisible = this.visible
        this.visible = visible
        if (!oldVisible && visible)
            displayBatchedResults()
    }
    
    private void displayBatchedResults() {
        if (!visible)
            return
        
        List<UIResultEvent> copy
        synchronized (pendingResults) {
            if (pendingResults.isEmpty())
                return
            copy = new ArrayList<>(pendingResults)
            pendingResults.clear()
        }
        
        for(UIResultEvent event : copy) {
            if (uiSettings.excludeLocalResult &&
                    core.fileManager.rootToFiles.containsKey(event.infohash))
                continue
            def bucket = hashBucket.get(event.infohash)
            if (bucket == null) {
                bucket = new HashBucket()
                hashBucket[event.infohash] = bucket
            }

            SenderBucket senderBucket = sendersBucket.get(event.sender)
            if (senderBucket == null) {
                senderBucket = new SenderBucket(event.sender, event.profileHeader, senders.size())
                sendersBucket[event.sender] = senderBucket
                senders << senderBucket
            }

            Set sourceBucket = sourcesBucket.get(event.infohash)
            if (sourceBucket == null) {
                sourceBucket = new HashSet()
                sourcesBucket.put(event.infohash, sourceBucket)
            }
            sourceBucket.addAll(event.sources)

            bucket.add event
            senderBucket.results << event
            
            view.addResultToDetailMaps(event)
        }
        
        synchronized (allResults2) {
            for (UIResultEvent event : copy) {
                InfoHash ih = event.getInfohash()
                if (allResults2.add(ih) && filter(ih))
                    results2.add(ih)
            }
        }
        view.addPendingResults()
        view.updateResultsTable2()
    }
    
    private static class HashBucket {
        private final AtomicReference<UIResultEvent> first = new AtomicReference<>()
        private final Set<UIResultEvent> events = new HashSet<>()
        private final Map<Persona, PersonaOrProfile> senders = new HashMap<>()
        
        private void add(UIResultEvent event) {
            first.compareAndSet(null, event)
            events.add(event)
            senders.put(event.sender, new ResultPOP(event))
        }
        
        UIResultEvent firstEvent() {
            first.get()
        }
        
        Set<UIResultEvent> getResults() {
            events
        }
        
        Set<Persona> getSenders() {
            senders.keySet()
        }
        
        Collection<PersonaOrProfile> getPOPs() {
            senders.values()
        }
        
        String getName() {
            first.get().name
        }
        
        long getSize() {
            first.get().size
        }
        
        int sourceCount() {
            senders.size()
        }
        
        int commentCount() {
            int count = 0
            for (UIResultEvent event : events) {
                if (event.comment != null)
                    count++
            }
            count
        }
        
        int certificateCount() {
            int count = 0
            for (UIResultEvent event : events)
                count += event.certificates
            count
        }
        
        int chatCount() {
            int count = 0
            for (UIResultEvent event : events) {
                if (event.chat)
                    count++
            }
            count
        }
        
        int feedCount() {
            int count = 0
            for (UIResultEvent event : events) {
                if (event.feed)
                    count++
            }
            count
        }
        
        int collectionsCount() {
            int count = 0
            for (UIResultEvent event : events) {
                count += event.collections.size()
            }
            count
        }
    }
    
    static class SenderBucket implements PersonaOrProfile {
        final Persona sender
        private final Icon avatar
        private final MWProfileHeader profileHeader
        private final int rowIdx
        final List<UIResultEvent> results = []
        private int lastUpdatedIdx
        
        SenderBucket(Persona sender, MWProfileHeader profileHeader,int rowIdx) {
            this.sender = sender
            this.rowIdx = rowIdx
            this.profileHeader = profileHeader
            if (profileHeader != null) {
                Icon icon = null
                try {
                    icon = new ThumbnailIcon(profileHeader.getThumbNail())
                } catch (IOException iox) {}
                avatar = icon
            } else {
                avatar = null
            }
        }
        
        Persona getPersona() {
            sender
        }

        Icon getThumbnail() {
            avatar
        }
        
        MWProfileHeader getHeader() {
            profileHeader
        }
        
        List<UIResultEvent> getPendingResults() {
            if (lastUpdatedIdx == results.size())
                return Collections.emptyList()
            def rv = results.subList(lastUpdatedIdx, results.size())
            lastUpdatedIdx = results.size()
            return rv
        }
    }
    
    private class Filterer extends SwingWorker<Void, InfoHash> {
        private volatile boolean cancelled
        
        void cancel() {
            cancelled = true
        }
        
        @Override
        protected Void doInBackground() {
            allResults2.parallelStream().
                    filter({filter(it)}).
                    forEach({publish(it)})
            return null
        }
        
        @Override
        protected void process(List<InfoHash> chunks) {
            if (cancelled)
                return
            results2.addAll(chunks)
        }
        
        @Override
        protected void done() {
            if (cancelled)
                return
            setClearFilterActionEnabled(true)
            view.clearSelections()
            view.refreshResults()
        }
    }
}