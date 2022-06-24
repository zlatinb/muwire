package com.muwire.gui

import com.muwire.core.SharedFile

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import java.text.Collator

class LibraryTreeModel extends DefaultTreeModel {
    
    LibraryTreeModel(TreeNode root) {
        super(root)
    }
    
    TreeNode addToTree(SharedFile sharedFile) {
        List<File> parents = getParents(sharedFile)
        LibraryTreeNode node = root
        for (File path : parents) {
            def key = new InterimTreeNode(path)
            def child = node.getByKey(key)
            if (child == null) {
                child = new LibraryTreeNode()
                child.setUserObject(key)
                node.addDescendant(child)
            }
            node = child
        }
        
        def leaf = new LibraryTreeNode(sharedFile)
        node.addDescendant(leaf)
        leaf
    }
    
    void removeFromTree(SharedFile sharedFile, boolean deleted) {
        List<File> parents = getParents(sharedFile)
        LibraryTreeNode node = root
        
        for (File path : parents) {
            def key = new InterimTreeNode(path)
            def child = node.getByKey(key)
            if (child == null) {
                if (deleted)
                    return
                throw new IllegalStateException()
            }
            node = child
        }
        def leaf = node.getByKey(sharedFile)
        while(true) {
            def parent = leaf.getParent()
            leaf.removeFromParent()
            if (parent.getChildCount() == 0 && parent != root)
                leaf = parent
            else
                break
        }
    }
    
    private List<File> getParents(SharedFile sharedFile) {
        List<File> parents = new ArrayList<>()
        File tmp = sharedFile.file.getParentFile()
        while(tmp.getParent() != null) {
            parents << tmp
            tmp = tmp.getParentFile()
        }
        Collections.reverse(parents)
        parents
    }
    
    static class LibraryTreeNode extends SortedTreeNode<SharedFile> {
        
        LibraryTreeNode() {
            super()
        }
        
        LibraryTreeNode(SharedFile sharedFile) {
            super(sharedFile)
        }
        
        @Override
        protected String getStringName() {
            def object = getUserObject()
            if (object instanceof SharedFile)
                return object.getFile().getName()
            else
                return object.toString()
        }
    }
}
