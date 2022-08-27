package com.muwire.core.util

import java.nio.file.Path

class PathTree<T> {
    
    private final Node root = new Node()
    private final Map<Path, Node> pathNodeMap = new HashMap<>()
    
    synchronized void add(Path path, T value) {
        Node parent = root
        for (int i = 0; i < path.getNameCount(); i++) {
            Path subPath = path.subpath(0, i + 1)
            Node newNode = pathNodeMap[subPath]
            if (newNode == null) {
                newNode = new Node()
                newNode.path = subPath
                newNode.parent = parent
                parent.addChild(newNode)
                pathNodeMap[subPath] = newNode
            }
            parent = newNode
        }
        parent.value = value
    }
    
    synchronized void traverse(PathTreeCallback<T> cb) {
        doTraverse(root, cb)
    }
    
    synchronized void traverse(Path from, PathTreeCallback<T> cb) {
        Node node = pathNodeMap[from]
        if (node == null)
            return
        doTraverse(node, cb)
    }
    
    private synchronized void doTraverse(Node from, PathTreeCallback<T> cb) {
        if (from.children.length == 0) {
            cb.onLeaf(from.path, from.value)
            return
        }
        
        if (from != root)
            cb.onDirectoryEnter(from.path)
        for (Node child : from.children)
            doTraverse(child, cb)
        if (from != root)
            cb.onDirectoryLeave()
    }
    
    synchronized void list(Path from, PathTreeListCallback<T> cb) {
        Node node
        if (from == null)
            node = root
        else
            node = pathNodeMap[from]
        
        for (Node child : node.children) {
            if (child.children.length == 0)
                cb.onLeaf(child.path, child.value)
            else
                cb.onDirectory(child.path)
        }
    }
    
    private class Node {
        Node parent
        Path path
        T value
        Node[] children = EMPTY_CHILDREN
        
        int hashCode() {
            Objects.hash(path)
        }
        
        boolean equals(Object o) {
            if (!(o instanceof Node))
                return false
            Node other = (Node)o
            path == other.path
        }
        
        private void addChild(Node child) {
            Set<Node> unique = new HashSet<>()
            unique.addAll(children)
            unique.add(child)
            children = unique.toArray(children)
        }
    }
    
    private static final Node[] EMPTY_CHILDREN = new Node[0]
}
