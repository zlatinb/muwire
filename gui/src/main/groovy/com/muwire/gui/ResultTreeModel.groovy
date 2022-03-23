package com.muwire.gui

import com.muwire.core.search.UIResultEvent

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

class ResultTreeModel extends DefaultTreeModel {
    
    ResultTreeModel(TreeNode root) {
        super(root)
    }

    void addToTree(UIResultEvent event) {
        def node = root
        if (event.path == null || event.path.length == 0) {
            def child = new DefaultMutableTreeNode()
            child.setUserObject(event)
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
                if (Objects.equals(node.getChildAt(i).getUserObject(), nodeData)) {
                    elementNode = node.getChildAt(i)
                    break
                }
            }
            if (elementNode == null) {
                elementNode = new DefaultMutableTreeNode()
                elementNode.setUserObject(nodeData)
                node.add(elementNode)
            }
            elementNode.getUserObject().addResult(event)
            node = elementNode
        }

        def fileNode = new DefaultMutableTreeNode()
        fileNode.setUserObject(event)
        node.add(fileNode)
    }
}
