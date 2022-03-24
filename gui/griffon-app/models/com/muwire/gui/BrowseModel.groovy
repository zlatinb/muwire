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
import javax.swing.tree.TreeNode

@ArtifactProviderFor(GriffonModel)
class BrowseModel {
    @MVCMember @Nonnull
    BrowseView view
    
    Persona host
    @Observable BrowseStatus status
    @Observable boolean downloadActionEnabled
    @Observable boolean viewDetailsActionEnabled
    @Observable int totalResults
    @Observable int resultCount
    @Observable boolean filterEnabled
    @Observable boolean clearFilterEnabled
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
    private final List<TreeNode> topLevelNodes = new ArrayList<>()
    
    void mvcGroupInit(Map<String,String> args) {
        root = new ResultTreeModel.MutableResultNode()
        resultsTreeModel = new ResultTreeModel(root)
    }
    
    void cacheTopTreeLevel() {
        for(int i = 0; i < root.getChildCount(); i ++)
            topLevelNodes.add(root.getChildAt(i))
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
        filterer?.cancel()
        results.clear()
        root.removeAllChildren()
        view.clearForFilter()
        view.refreshResults() 
        if (filter != null) {
            setFilterEnabled(false)
            setClearFilterEnabled(false)
            filterer = new Filterer()
            filterer.execute()
        } else {
            synchronized (allResults) {
                results.addAll(allResults)
            }
            for(TreeNode topLevel : topLevelNodes) {
                root.add(topLevel)
            }
            view.refreshResults()
            view.expandUnconditionally()
        }
    }
    
    private class Filterer extends SwingWorker<Void, UIResultEvent> {
        private volatile boolean cancelled;
        
        void cancel() {
            cancelled = true
        }
        
        @Override
        protected Void doInBackground() throws Exception {
            allResults.stream().parallel().
                    filter({filter(it)}).
                    forEach({publish(it)})
            return null
        }
        
        @Override
        protected void process(List<UIResultEvent> chunks) {
            if (cancelled || chunks.isEmpty())
                return
            results.addAll(chunks)
            for (UIResultEvent result : chunks)
                resultsTreeModel.addToTree(result)
            view.refreshResults()
            view.expandUnconditionally()
        }
        
        @Override
        protected void done() {
            if (cancelled)
                return
            view.refreshResults()
            view.expandUnconditionally()
            setFilterEnabled(true)
            setClearFilterEnabled(true)
        }
    }
}