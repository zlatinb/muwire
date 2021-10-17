package com.muwire.gui

import com.muwire.core.InfoHash

import javax.annotation.Nonnull

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.search.UIResultEvent

import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

import javax.swing.SwingWorker
import javax.swing.tree.DefaultMutableTreeNode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@ArtifactProviderFor(GriffonModel)
class SearchTabModel {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    SearchTabView view
    
    @Observable boolean downloadActionEnabled
    @Observable boolean trustButtonsEnabled
    @Observable boolean browseActionEnabled
    @Observable boolean browseCollectionsActionEnabled
    @Observable boolean viewCommentActionEnabled
    @Observable boolean viewCertificatesActionEnabled
    @Observable boolean chatActionEnabled
    @Observable boolean subscribeActionEnabled
    @Observable boolean viewCollectionsActionEnabled
    @Observable boolean messageActionEnabled
    @Observable boolean groupedByFile
    

    Core core
    UISettings uiSettings
    String uuid
    
    boolean visible
    private boolean dirty
    List<UIResultEvent> pendingResults = Collections.synchronizedList(new ArrayList<>())
    javax.swing.Timer timer = new javax.swing.Timer(500, {displayBatchedResults()})
    
    def senders = []
    def results = []
    def hashBucket = new ConcurrentHashMap<InfoHash, HashBucket>()
    def sourcesBucket = [:]
    def sendersBucket = new LinkedHashMap<>()
    
    def results2 = []
    def allResults2 = new LinkedHashSet()
    def senders2 = []
    volatile String[] filter
    volatile Filterer filterer
    @Observable boolean clearFilterActionEnabled
    

    ResultTreeModel treeModel
    DefaultMutableTreeNode root
    boolean treeVisible = true
    
    void mvcGroupInit(Map<String, String> args) {
        root = new DefaultMutableTreeNode()
        treeModel = new ResultTreeModel(root)
        core = mvcGroup.parentGroup.model.core
        uiSettings = application.context.get("ui-settings")
        timer.start()
        mvcGroup.parentGroup.model.results[UUID.fromString(uuid)] = mvcGroup
    }

    void mvcGroupDestroy() {
        timer.stop()
        mvcGroup.parentGroup.model.results.remove(uuid)
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
    
    private void displayBatchedResults() {
        List<UIResultEvent> copy
        synchronized (pendingResults) {
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

            def senderBucket = sendersBucket.get(event.sender)
            if (senderBucket == null) {
                senderBucket = []
                sendersBucket[event.sender] = senderBucket
                senders.clear()
                senders.addAll(sendersBucket.keySet())
            }

            Set sourceBucket = sourcesBucket.get(event.infohash)
            if (sourceBucket == null) {
                sourceBucket = new HashSet()
                sourcesBucket.put(event.infohash, sourceBucket)
            }
            sourceBucket.addAll(event.sources)

            bucket.add event
            senderBucket << event
        }
        
        
        if (visible) {
            if (!copy.isEmpty() || dirty) {
                results2.clear()
                synchronized (allResults2) {
                    allResults2.addAll(hashBucket.keySet())
                    allResults2.stream().filter({ InfoHash ih -> filter(ih) }).forEach({ results2.add it })
                }
                view.refreshResults()
                dirty = false
            }
        } else if (!copy.isEmpty())
            dirty = true
    }
    
    private static class HashBucket {
        private final AtomicReference<UIResultEvent> first = new AtomicReference<>()
        private final Set<UIResultEvent> events = new HashSet<>()
        private final Set<Persona> senders = new HashSet<>()
        
        private void add(UIResultEvent event) {
            first.compareAndSet(null, event)
            events.add(event)
            senders.add(event.sender)
        }
        
        UIResultEvent firstEvent() {
            first.get()
        }
        
        Set<UIResultEvent> getResults() {
            events
        }
        
        Set<Persona> getSenders() {
            senders
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