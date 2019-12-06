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
                existing.parent = current
                fileToNode.put(element, existing)
                current.children.add(existing)
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
        node.parent.children.remove(node)
        if (node.parent.children.isEmpty() && node.parent != root)
            remove(node.parent.file)
        def copy = new ArrayList(node.children)
        for (TreeNode child : copy)
            remove(child.file)
        true
    }    
    
    synchronized void traverse(FileTreeCallback<T> callback) {
        doTraverse(root, callback);
    }
    
    private void doTraverse(TreeNode<T> node, FileTreeCallback<T> callback) {
        println "traversing $node"
        boolean leave = false
        if (node.file != null) {
            println "file is $node.file"
            if (node.file.isFile())
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
    
    public static class TreeNode<T> {
        TreeNode parent
        File file
        T value;
        final Set<TreeNode> children = new HashSet<>()
        
        public int hashCode() {
            Objects.hash(file)
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof TreeNode))
                return false
            TreeNode other = (TreeNode)o
            file == other.file
        }
    }
}
