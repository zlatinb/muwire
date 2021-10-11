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
    

    ResultTreeModel treeModel
    DefaultMutableTreeNode root
    boolean treeVisible = true
    
    void mvcGroupInit(Map<String, String> args) {
        root = new DefaultMutableTreeNode()
        treeModel = new ResultTreeModel(root)
        core = mvcGroup.parentGroup.model.core
        uiSettings = application.context.get("ui-settings")
        mvcGroup.parentGroup.model.results[UUID.fromString(uuid)] = mvcGroup
    }

    void mvcGroupDestroy() {
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
        filterer?.cancel()
        if (filter != null) {
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
        runInsideUIAsync {
            batch.each {
                if (uiSettings.excludeLocalResult &&
                    core.fileManager.rootToFiles.containsKey(it.infohash))
                    return
                def bucket = hashBucket.get(it.infohash)
                if (bucket == null) {
                    bucket = new HashBucket()
                    hashBucket[it.infohash] = bucket
                }
                
                def senderBucket = sendersBucket.get(it.sender)
                if (senderBucket == null) {
                    senderBucket = []
                    sendersBucket[it.sender] = senderBucket
                    senders.clear()
                    senders.addAll(sendersBucket.keySet())
                }

                Set sourceBucket = sourcesBucket.get(it.infohash)
                if (sourceBucket == null) {
                    sourceBucket = new HashSet()
                    sourcesBucket.put(it.infohash, sourceBucket)
                }
                sourceBucket.addAll(it.sources)

                bucket.add it
                senderBucket << it
                
            }
            results2.clear()
            synchronized(allResults2) {
                allResults2.addAll(hashBucket.keySet())
                allResults2.stream().filter({ InfoHash ih -> filter(ih) }).forEach({ results2.add it })
            }
            view.refreshResults()
        }
    }
    
    private static class HashBucket {
        private final Set<UIResultEvent> events = new HashSet<>()
        private final Set<Persona> senders = new HashSet<>()
        private String cachedName
        private long cachedSize
        private void add(UIResultEvent event) {
            events.add(event)
            senders.add(event.sender)
        }
        
        Set<UIResultEvent> getResults() {
            events
        }
        
        Set<Persona> getSenders() {
            senders
        }
        
        String getName() {
            if (cachedName == null) {
                cachedName = events.first().name
            }
            cachedName
        }
        
        long getSize() {
            if (cachedSize == 0) {
                cachedSize = events.first().size
            }
            cachedSize
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
    
    private class Filterer extends SwingWorker<List<InfoHash>, InfoHash> {
        private volatile boolean cancelled
        
        void cancel() {
            cancelled = true
        }
        
        @Override
        protected List<InfoHash> doInBackground() {
            synchronized(allResults2) {
                for (InfoHash infoHash : allResults2) {
                    if (cancelled)
                        break
                    if (filter(infoHash))
                        publish(infoHash)
                }
            }
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
            view.refreshResults()
        }
    }
}