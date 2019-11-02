package com.muwire.core.files

import java.util.concurrent.ConcurrentHashMap

class FileTree {
    
    private final TreeNode root = new TreeNode()
    private final Map<File, TreeNode> fileToNode = new ConcurrentHashMap<>()
    
    void add(File file) {
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
    }

    boolean remove(File file) {
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
    
    public static class TreeNode {
        TreeNode parent
        File file
        final Set<TreeNode> children = new HashSet<>()
        
        public int hashCode() {
            file.hashCode()
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof TreeNode))
                return false
            TreeNode other = (TreeNode)o
            file == other.file
        }
    }
}
