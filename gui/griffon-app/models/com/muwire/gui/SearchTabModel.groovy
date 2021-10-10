package com.muwire.gui

import com.muwire.core.InfoHash

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JTable

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.search.UIResultEvent

import griffon.core.artifact.GriffonModel
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

@ArtifactProviderFor(GriffonModel)
class SearchTabModel {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    
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
    def hashBucket = new HashMap<InfoHash, HashBucket>()
    def sourcesBucket = [:]
    def sendersBucket = new LinkedHashMap<>()
    
    def results2 = []
    def senders2 = []
    

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

    void handleResult(UIResultEvent e) {
        if (uiSettings.excludeLocalResult &&
            core.fileManager.rootToFiles.containsKey(e.infohash))
            return
        runInsideUIAsync {
            def bucket = hashBucket.get(e.infohash)
            if (bucket == null) {
                bucket = new HashBucket()
                hashBucket[e.infohash] = bucket
            }
            bucket.add(e)

            def senderBucket = sendersBucket.get(e.sender)
            if (senderBucket == null) {
                senderBucket = []
                sendersBucket[e.sender] = senderBucket
                senders.clear()
                senders.addAll(sendersBucket.keySet())
            }
            senderBucket << e
            
            Set sourceBucket = sourcesBucket.get(e.infohash)
            if (sourceBucket == null) {
                sourceBucket = new HashSet()
                sourcesBucket.put(e.infohash, sourceBucket)
            }
            sourceBucket.addAll(e.sources)
            
            results2.clear()
            results2.addAll(hashBucket.keySet())

            JTable table = builder.getVariable("senders-table")
            table.model.fireTableDataChanged()
            table = builder.getVariable("results-table2")
            table.model.fireTableDataChanged()
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
            results2.addAll(hashBucket.keySet())
            JTable table = builder.getVariable("senders-table")
            int selectedRow = table.getSelectedRow()
            table.model.fireTableDataChanged()
            table.selectionModel.setSelectionInterval(selectedRow, selectedRow)
            table = builder.getVariable("results-table2")
            selectedRow = table.getSelectedRow() 
            table.model.fireTableDataChanged()
            table.selectionModel.setSelectionInterval(selectedRow, selectedRow)
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
}