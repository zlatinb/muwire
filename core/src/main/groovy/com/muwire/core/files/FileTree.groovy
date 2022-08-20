package com.muwire.core.files

import java.util.concurrent.ConcurrentHashMap

class FileTree<T> {
    
    private final TreeNode root = new TreeNode()
    private final Map<File, TreeNode> fileToNode = new ConcurrentHashMap<>()
    
    synchronized void add(File file, T value) {
        List<File> path = new ArrayList<>()
        path.add(file)
        while (file.getParentFile() != null) {
            path.add(file.getParentFile())
            file = file.getParentFile()
        }
        
        Collections.reverse(path)
        
        TreeNode current = root
        for (File element : path) {
            TreeNode existing = fileToNode.get(element)
            if (existing == null) {
                existing = new TreeNode()
                existing.file = element
                existing.isFile = element.isFile()
                existing.parent = current
                fileToNode.put(element, existing)
                current.addChild(existing)
            }
            current = existing
        }
        current.value = value;
    }

    synchronized boolean remove(File file) {
        TreeNode node = fileToNode.remove(file)
        if (node == null) {
            return false
        }
        node.parent.removeChild(node)
        if (node.parent.children.length == 0 && node.parent != root)
            remove(node.parent.file)
        def copy = new ArrayList()
        copy.addAll node.children
        for (TreeNode child : copy)
            remove(child.file)
        true
    }    
    
    T get(File file) {
        fileToNode.get(file)?.value
    }
    
    boolean contains(File file) {
        fileToNode.containsKey(file)
    }
    
    synchronized void traverse(FileTreeCallback<T> callback) {
        doTraverse(root, callback);
    }

    synchronized void traverse(File from, FileTreeCallback<T> callback) {
        if (from == null) {
            doTraverse(root, callback);
        } else {
            TreeNode node = fileToNode.get(from);
            if (node == null)
                return
            doTraverse(node, callback);
        }
    }
        
    private void doTraverse(TreeNode<T> node, FileTreeCallback<T> callback) {
        boolean leave = false
        if (node.file != null) {
            if (node.isFile)
                callback.onFile(node.file, node.value)
            else {
                leave = true
                callback.onDirectoryEnter(node.file)
            }
        }
        
        node.children.each { 
            doTraverse(it, callback)
        }        
        
        if (leave)
            callback.onDirectoryLeave()
    }
    
    synchronized void list(File parent, FileListCallback<T> callback) {
        TreeNode<T> node
        if (parent == null)
            node = root
        else 
            node = fileToNode.get(parent)
            
        node.children.each { 
            if (it.isFile)
                callback.onFile(it.file, it.value)
            else
                callback.onDirectory(it.file)
        }
    }
    
    synchronized File commonAncestor() {
        TreeNode current = root
        while(current.children.length == 1)
            current = current.children[0]
        current.file
    }
    
    public static class TreeNode<T> {
        TreeNode parent
        File file
        boolean isFile
        T value;
        TreeNode[] children = EMPTY_CHILDREN
        
        public int hashCode() {
            Objects.hash(file)
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof TreeNode))
                return false
            TreeNode other = (TreeNode)o
            file == other.file
        }
        
        private void addChild(TreeNode child) {
            Set<TreeNode> unique = new HashSet<>()
            unique.addAll(children)
            unique.add(child)
            children = unique.toArray(children)
        }
        
        private void removeChild(TreeNode child) {
            Set<TreeNode> unique = new HashSet<>()
            unique.addAll(children)
            unique.remove(child)
            if (unique.isEmpty())
                children = EMPTY_CHILDREN
            else
                children = unique.toArray(new TreeNode[0])
        }
    }
    
    private static final TreeNode[] EMPTY_CHILDREN = new TreeNode[0]
}
