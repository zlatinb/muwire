package com.muwire.gui

import com.muwire.core.search.UIBrowseDirEvent
import com.muwire.core.search.UIResultEvent

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode
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
            }
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
            }
            elementNode.getUserObject().addResult(event)
            node = elementNode
        }

        def fileNode = new MutableResultNode(event)
        TreeNode placeHolder = node.getByKey(ResultTreeRenderer.PLACEHOLDER)
        if (placeHolder != null)
            node.remove(placeHolder)
        node.addDescendant(fileNode)
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
