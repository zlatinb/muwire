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


    TreeModel resultsTreeModel
    DefaultMutableTreeNode root
    boolean treeVisible = true
    
    volatile String[] filter
    volatile Filterer filterer
    
    void mvcGroupInit(Map<String,String> args) {
        root = new DefaultMutableTreeNode()
        resultsTreeModel = new DefaultTreeModel(root)
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
                    addToTree(result)
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
                addToTree(result)
        }
        
        @Override
        protected void done() {
            if (cancelled)
                return
            view.refreshResults()
            view.expandUnconditionally()
        }
    }
    
    void addToTree(UIResultEvent event) {
        def node = root
        if (event.path == null || event.path.length == 0) {
            def child = new DefaultMutableTreeNode()
            child.setUserObject(event.name)
            node.add(child)
            return
        }
        
        List<String> elements = new ArrayList<>()
        for (String element : event.path)
            elements << element
        
        String hiddenRoot = elements.remove(0)
        for (String element : elements) {
            def nodeData = new ResultTreeRenderer.ResultTreeNode(hiddenRoot, element) 
            def elementNode = null
            for(int i = 0; i < node.childCount; i++) {
                if (node.getChildAt(i).getUserObject() == nodeData) {
                    elementNode = node.getChildAt(i)
                    break
                }
            }
            if (elementNode == null) {
                elementNode = new DefaultMutableTreeNode()
                elementNode.setUserObject(nodeData)
                node.add(elementNode)
            }
            node = elementNode
        }
     
        def fileNode = new DefaultMutableTreeNode()
        fileNode.setUserObject(event)
        node.add(fileNode)
    }
}