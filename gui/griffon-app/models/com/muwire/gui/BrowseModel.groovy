package com.muwire.gui

import com.muwire.core.Persona
import com.muwire.core.search.BrowseStatusEvent
import com.muwire.core.search.UIResultBatchEvent
import com.muwire.core.search.UIResultEvent
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

import com.muwire.core.search.BrowseStatus

import javax.annotation.Nonnull
import javax.swing.SwingWorker
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel

@ArtifactProviderFor(GriffonModel)
class BrowseModel {
    @MVCMember @Nonnull
    BrowseView view
    
    Persona host
    @Observable BrowseStatus status
    @Observable boolean downloadActionEnabled
    @Observable boolean viewCommentActionEnabled
    @Observable boolean viewCollectionsActionEnabled
    @Observable boolean viewCertificatesActionEnabled
    @Observable boolean chatActionEnabled
    @Observable int totalResults
    @Observable int resultCount
    @Observable boolean filterEnabled
    volatile UUID uuid
    
    def results = []
    List<UIResultEvent> allResults = []
    
    boolean visible = true
    boolean dirty
    List<UIResultBatchEvent> pendingResults = Collections.synchronizedList(new ArrayList<>())
    List<BrowseStatusEvent> pendingStatuses = Collections.synchronizedList(new ArrayList<>())


    ResultTreeModel resultsTreeModel
    DefaultMutableTreeNode root
    boolean treeVisible = true
    
    volatile String[] filter
    volatile Filterer filterer
    
    void mvcGroupInit(Map<String,String> args) {
        root = new DefaultMutableTreeNode()
        resultsTreeModel = new ResultTreeModel(root)
    }
    
    private boolean filter(UIResultEvent result) {
        if (filter == null)
            return true
        String name = result.getFullPath().toLowerCase()
        boolean contains = true
        for (String keyword : filter) {
            contains &= name.contains(keyword)
        }
        contains
    }
    
    void filterResults() {
        results.clear()
        root.removeAllChildren()
        filterer?.cancel()
        if (filter != null) {
            filterer = new Filterer()
            filterer.execute()
        } else {
            synchronized (allResults) {
                results.addAll(allResults)
                for(UIResultEvent result : allResults)
                    resultsTreeModel.addToTree(result)
            }
            view.refreshResults()
            view.expandUnconditionally()
        }
    }
    
    private class Filterer extends SwingWorker<List<UIResultEvent>, UIResultEvent> {
        private volatile boolean cancelled;
        
        void cancel() {
            cancelled = true
        }
        
        @Override
        protected List<UIResultEvent> doInBackground() throws Exception {
            synchronized (allResults) {
                for (UIResultEvent result : allResults) {
                    if (cancelled)
                        break
                    if (filter(result))
                        publish(result)
                }
            }
        }
        
        @Override
        protected void process(List<UIResultEvent> chunks) {
            if (cancelled)
                return
            results.addAll(chunks)
            for (UIResultEvent result : chunks)
                resultsTreeModel.addToTree(result)
        }
        
        @Override
        protected void done() {
            if (cancelled)
                return
            view.refreshResults()
            view.expandUnconditionally()
        }
    }
}