package com.muwire.gui

import com.muwire.core.search.UIBrowseDirEvent
import com.muwire.core.search.UIResultEvent

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import java.text.Collator

class ResultTreeModel extends DefaultTreeModel {
    
    ResultTreeModel(TreeNode root) {
        super(root)
    }
    
    void addToTree(UIBrowseDirEvent event) {
        MutableResultNode node = root

        List<String> elements = new ArrayList<>()
        for (String element : event.path)
            elements << element

        String hiddenRoot = elements.remove(0)
        for (String element : elements) {
            def nodeData = new ResultTreeRenderer.ResultTreeNode(hiddenRoot, element)
            def elementNode = node.getByKey(nodeData)

            if (elementNode == null) {
                elementNode = new MutableResultNode()
                elementNode.setUserObject(nodeData)
                node.addDescendant(elementNode)
            } else
                removePlaceholder(elementNode)
            node = elementNode
        }
     
        node.addDescendant(new PlaceholderNode())
    }

    void addToTree(UIResultEvent event) {
        MutableResultNode node = root
        if (event.path == null || event.path.length == 0) {
            def child = new MutableResultNode(event)
            node.addDescendant(child)
            return
        }

        List<String> elements = new ArrayList<>()
        for (String element : event.path)
            elements << element

        String hiddenRoot = elements.remove(0)
        for (String element : elements) {
            def nodeData = new ResultTreeRenderer.ResultTreeNode(hiddenRoot, element)
            def elementNode = node.getByKey(nodeData)
            
            if (elementNode == null) {
                elementNode = new MutableResultNode()
                elementNode.setUserObject(nodeData)
                node.addDescendant(elementNode)
            } else {
                removePlaceholder(elementNode)
            }
            elementNode.getUserObject().addResult(event)
            node = elementNode
        }

        def fileNode = new MutableResultNode(event)
        removePlaceholder(node)
        node.addDescendant(fileNode)
    }
    
    private static void removePlaceholder(SortedTreeNode node) {
        TreeNode placeHolder = node.getByKey(ResultTreeRenderer.PLACEHOLDER)
        if (placeHolder != null)
            node.remove(placeHolder)
    }
    
    List<String> getPathFromRoot(TreePath treePath) {
        
        Object [] objects = treePath.getPath()
        
        List<String> rv = new ArrayList<>()
        String hiddenRoot = null
        for (int i = 1; i < objects.length; i++) {
            Object userObject = objects[i].getUserObject()
            if (userObject instanceof ResultTreeRenderer.ResultTreeNode) {
                hiddenRoot = userObject.getHiddenRoot()
                rv << userObject.toString()
            }
        }
        
        rv.add(0, hiddenRoot)
        rv
    }
    
    static class MutableResultNode extends SortedTreeNode<UIResultEvent> {
        MutableResultNode() {
            super()
        }
        
        MutableResultNode(UIResultEvent event) {
            super(event)
        }

        @Override
        protected String getStringName() {
            def object = getUserObject()
            if (object instanceof UIResultEvent)
                return object.getName()
            else
                return object.toString()
        }
    }
    
    static class PlaceholderNode extends SortedTreeNode {
        PlaceholderNode() {
            super(ResultTreeRenderer.PLACEHOLDER)
        }
        
        @Override
        protected String getStringName() {
            "N/A"
        }
    }
}
