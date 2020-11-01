package com.muwire.gui

import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath

import com.muwire.core.collections.PathTree



class TreeUtil {

    private TreeUtil() {
    }
    
    public static void copy(DefaultMutableTreeNode jtreeNode, PathTree.PathNode pathNode) {
        jtreeNode.setUserObject(pathNode.path)
        pathNode.children.each {
            MutableTreeNode newChild = new DefaultMutableTreeNode()
            jtreeNode.add(newChild)
            copy(newChild, it)
        }
    }
    
    public static void expand(JTree tree) {
        expandNode(tree, tree.model.root)
    }
    
    private static void expandNode(JTree tree, DefaultMutableTreeNode node) {
        List<DefaultMutableTreeNode> copy = Collections.list(node.children())
        for(def child : copy)
            expandNode(tree, child)
        
        def path = new TreePath(node.getPath())
        tree.expandPath(path)
    }
}
