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
        List<File> parents = getParents(sharedFile.getFile())
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

    /**
     * @param folder to remove from the tree
     * @return true if tree was modified
     */
    boolean removeFromTree(File folder) {
        def node = findParentNode(folder, true)
        if (node == null)
            return false
        def key = new InterimTreeNode(folder)
        def child = node.getByKey(key)
        if (child == null)
            return false
        while(true) {
            def parent = child.getParent()
            child.removeFromParent()
            if (parent.getChildCount() == 0 && parent != root)
                child = parent
            else
                break
        }
        return true
    }
    
    List<SharedFile> getFilesInFolder(File folder) {
        def node = findParentNode(folder, false)
        if (node == null)
            return Collections.emptyList()
        def key = new InterimTreeNode(folder)
        def child = node.getByKey(key)
        List<SharedFile> rv = []
        TreeUtil.getLeafs(child, rv)
        rv
    }
    
    void removeFromTree(SharedFile sharedFile, boolean deleted) {
        def node = findParentNode(sharedFile.getFile(), deleted)
        if (node == null)
            return
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
    
    private LibraryTreeNode findParentNode(File file, boolean deleted) {
        List<File> parents = getParents(file)
        LibraryTreeNode node = root

        for (File path : parents) {
            def key = new InterimTreeNode(path)
            def child = node.getByKey(key)
            if (child == null) {
                if (deleted)
                    return null
                throw new IllegalStateException()
            }
            node = child
        }
        node
    }
    
    private static List<File> getParents(File sharedFile) {
        List<File> parents = new ArrayList<>()
        File tmp = sharedFile.getParentFile()
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
