package com.muwire.gui

import com.muwire.core.search.UIResultEvent

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode

class ResultTreeModel extends DefaultTreeModel {
    
    ResultTreeModel(TreeNode root) {
        super(root)
    }

    void addToTree(UIResultEvent event) {
        MutableResultNode node = root
        if (event.path == null || event.path.length == 0) {
            def child = new MutableResultNode(event)
            node.add(child)
            return
        }

        List<String> elements = new ArrayList<>()
        for (String element : event.path)
            elements << element

        String hiddenRoot = elements.remove(0)
        for (String element : elements) {
            def nodeData = new ResultTreeRenderer.ResultTreeNode(hiddenRoot, element)
            def elementNode = node.childrenMap.get(nodeData)
            
            if (elementNode == null) {
                elementNode = new MutableResultNode()
                elementNode.setUserObject(nodeData)
                node.add(elementNode)
            }
            elementNode.getUserObject().addResult(event)
            node = elementNode
        }

        def fileNode = new MutableResultNode(event)
        node.add(fileNode)
    }
    
    static class MutableResultNode extends DefaultMutableTreeNode {
        private final Map<Object, MutableResultNode> childrenMap
        MutableResultNode() {
            super()
            childrenMap = new HashMap<>()
        }
        
        MutableResultNode(UIResultEvent event) {
            super()
            childrenMap = Collections.emptyMap()
            setUserObject(event)
        }

        @Override
        void removeAllChildren() {
            if (!childrenMap.isEmpty())
                childrenMap.clear()
            super.removeAllChildren()
        }

        @Override
        void add(MutableTreeNode newChild) {
            MutableResultNode mrn = (MutableResultNode) newChild
            childrenMap.put(mrn.getUserObject(), mrn)
            super.add(newChild)
        }
    }
}
