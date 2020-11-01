package com.muwire.core.collections

class PathTree {

    private final Map<PathNode.Key, PathNode> keyToNode = new HashMap<>()

    final PathNode root
    PathTree(String root) {
        this.root = new PathNode(root, null)
        this.root.setUserObject(root)
        keyToNode.put(this.root.key(), this.root)
    }

    void add(List<String> paths, FileCollectionItem item) {
        PathNode current = null
        for (String path : paths) {
            if (current == null) {
                if (path == root.path) {
                    current = root
                    continue
                }
            }


            PathNode child = new PathNode(path, current)
            PathNode.Key childKey = child.key()
            if (!keyToNode.containsKey(childKey)) {
                keyToNode.put(childKey, child)
                current.children.add(child)
            }
            current = keyToNode.get(childKey)
            current.userObject = path
        }
        current?.userObject = item
    }
    
    public void traverse(Callback cb) {
        doTraverse(root, cb)
    }
    
    private void doTraverse(PathNode node, Callback cb) {
        if (node.children.isEmpty())
            cb.onFile(node.path)
        else {
            cb.onDirectoryEnter(node.path)
            for (PathNode child : node.children)
                doTraverse(child, cb)
            cb.onDirectoryLeave()
        }
    }

    static class PathNode {
        final String path
        final PathNode parent
        final Set<PathNode> children = new LinkedHashSet<>()
        private final int hashCode
        Object userObject

        PathNode(String path, PathNode parent) {
            this.parent = parent
            this.path = path
            this.hashCode = Objects.hash(path, parent)
        }


        Key key() {
            return new Key()
        }

        private class Key {

            private final PathNode node = PathNode.this

            public int hashCode() {
                hashCode
            }
            public boolean equals(Object o) {
                Key other = (Key)o
                Objects.equals(path, other.node.path) &&
                        Objects.equals(parent, other.node.parent)
            }
        }
    }
    
    public static interface Callback {
        public void onDirectoryEnter(String name)
        public void onDirectoryLeave()
        public void onFile(String name)
    }
}
