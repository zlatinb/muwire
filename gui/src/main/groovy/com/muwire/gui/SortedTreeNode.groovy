package com.muwire.gui

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode
import java.text.Collator

/**
 * A tree node that keeps it's children sorted.
 */
abstract class SortedTreeNode<T> extends DefaultMutableTreeNode implements Comparable<SortedTreeNode> {
    private final Map<Object, SortedTreeNode> childrenMap
    
    SortedTreeNode() {
        super()
        childrenMap = new HashMap<>()
    }
    
    SortedTreeNode(T element) {
        super()
        childrenMap = Collections.emptyMap()
        setUserObject(element)
    }

    @Override
    void removeAllChildren() {
        if (!childrenMap.isEmpty())
            childrenMap.clear()
        super.removeAllChildren()
    }
    
    @Override
    void remove(MutableTreeNode aChild) {
        if (!(aChild instanceof SortedTreeNode))
            throw new IllegalStateException()
        childrenMap.remove(aChild.getUserObject())
        super.remove(aChild)
    }
    
    SortedTreeNode getByKey(Object key) {
        childrenMap[key]
    }
    
    void addDescendant(SortedTreeNode<T> newChild) {
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
    
    protected abstract String getStringName()

    @Override
    final int compareTo(SortedTreeNode other) {
        Collator.getInstance().compare(getStringName(), other.getStringName())
    }
}
