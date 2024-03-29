package com.muwire.core.util

import java.nio.file.Path
import java.util.function.Function

/**
 * 
 * @param <T> type of object to store at leaf nodes
 * @param <I> type of object to store at interim nodes
 */
class PathTree<T,I> {

    private final Node root = new Node(null, null)
    private final Map<Path, Node> pathNodeMap = new HashMap<>()
    
    private final Function<Path, I> function
    
    PathTree() {
        this(null)
    }
    
    PathTree(Function<Path, I> function) {
        this.function = function
    }
    
    synchronized void add(Path path, T value) {
        Node parent = root
        for (int i = 0; i < path.getNameCount(); i++) {
            Path subPath = path.subpath(0, i + 1)
            Node newNode = pathNodeMap[subPath]
            if (newNode == null) {
                newNode = new Node(parent, subPath)
                if (function != null)
                    newNode.interimValue = function.apply(newNode.path)
                parent.addChild(newNode)
                pathNodeMap[subPath] = newNode
            }
            parent = newNode
        }
        parent.value = value
    }
    
    synchronized void traverse(PathTreeCallback<T, I> cb) {
        doTraverse(root, cb)
    }
    
    synchronized void traverse(Path from, PathTreeCallback<T, I> cb) {
        Node node = pathNodeMap[from]
        if (node == null)
            return
        doTraverse(node, cb)
    }
    
    private synchronized void doTraverse(Node from, PathTreeCallback<T,I> cb) {
        if (from.children.isEmpty()) {
            cb.onLeaf(from.path, from.value)
            return
        }
        
        if (from != root)
            cb.onDirectoryEnter(from.path, from.interimValue)
        for (Node child : from.children)
            doTraverse(child, cb)
        if (from != root)
            cb.onDirectoryLeave()
    }
    
    synchronized void list(Path from, PathTreeListCallback<T, I> cb) {
        Node node
        if (from == null)
            node = root
        else
            node = pathNodeMap[from]
        
        for (Node child : node.children) {
            if (child.children.isEmpty())
                cb.onLeaf(child.path, child.value)
            else
                cb.onDirectory(child.path, child.interimValue)
        }
    }
    
    private class Node {
        final Node parent
        final Path path
        final int hashcode
        
        T value
        I interimValue
        Set<Node> children = Collections.emptySet()
        
        Node(Node parent, Path path) {
            this.parent = parent
            this.path = path
            if (path != null)
                hashcode = path.hashCode()
            else
                hashcode = 0
        }
        
        int hashCode() {
            hashcode
        }
        
        boolean equals(Object o) {
            if (!(o instanceof Node))
                return false
            Node other = (Node)o
            path == other.path
        }
        
        private void addChild(Node child) {
            if (children == Collections.emptySet())
                children = new HashSet<>()
            children.add(child)
        }
    }
    
    
}
