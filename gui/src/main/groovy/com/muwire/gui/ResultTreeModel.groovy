package com.muwire.gui

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

    void addToTree(UIResultEvent event) {
        MutableResultNode node = root
        if (event.path == null || event.path.length == 0) {
            def child = new MutableResultNode(event)
            node.addResult(child)
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
                node.addResult(elementNode)
            }
            elementNode.getUserObject().addResult(event)
            node = elementNode
        }

        def fileNode = new MutableResultNode(event)
        node.addResult(fileNode)
    }
    
    static class MutableResultNode extends DefaultMutableTreeNode implements Comparable<MutableResultNode>{
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

        void addResult(MutableResultNode newChild) {
            childrenMap.put(newChild.getUserObject(), newChild)
            if (children == null)
                children = new Vector<>()
            Object [] elementData = children.elementData
            int idx = Arrays.binarySearch(elementData, 0, getChildCount(), newChild)
            if (idx >= 0)
                idx++
            else
                idx = - idx - 1
            insert(newChild, idx)
        }
        
        private String getStringName() {
            def object = getUserObject()
            if (object instanceof UIResultEvent)
                return object.getName()
            else
                return object.toString()
        }
    
        @Override
        int compareTo(MutableResultNode other) {
            Collator.getInstance().compare(getStringName(), other.getStringName())
        }
    }
}
