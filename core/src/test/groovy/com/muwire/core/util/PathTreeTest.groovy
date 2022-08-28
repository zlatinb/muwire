package com.muwire.core.util

import org.junit.Test

import java.nio.file.Path

class PathTreeTest {
    @Test
    void testList() {
        def tree = new PathTree()
        tree.add(Path.of("a","b","c"), "abc")
        
        def cb = new ListCB()
        tree.list(null, cb)
        assert cb.files.isEmpty()
        assert cb.dirs.size() == 1
        assert cb.dirs.contains(Path.of("a"))
        
        cb = new ListCB()
        tree.list(Path.of("a","b"), cb)
        assert cb.dirs.isEmpty()
        assert cb.files.size() == 1
        assert cb.files.contains("abc")
    }
    
    @Test
    void testTraverse() {
        def tree = new PathTree()
        tree.add(Path.of("a","b","c"), "abc")
        tree.add(Path.of("a", "d"), "ad") 
        
        def cb = new CB()
        tree.traverse(cb)
        assert cb.dirs.size() == 2
        assert cb.dirs.contains(Path.of("a"))
        assert cb.dirs.contains(Path.of("a","b"))
        assert cb.files.size() == 2
        assert cb.files[Path.of("a", "d")] == "ad"
        assert cb.files[Path.of("a", "b", "c")] == "abc"
        assert cb.leftDirs == 2
    }
    
    private static class ListCB implements PathTreeListCallback<String, Void> {
        Set<Path> dirs = new HashSet<>()
        Set<String> files = new HashSet<>()

        @Override
        void onLeaf(Path path, String value) {
            files.add(value)
        }

        @Override
        void onDirectory(Path path, Void value) {
            dirs.add(path)
        }
    }
    
    private static class CB implements PathTreeCallback<String, Void> {
        
        private Set<Path> dirs = new HashSet<>()
        private Map<Path, String> files = new HashMap<>()
        private int leftDirs

        @Override
        void onDirectoryEnter(Path path, Void value) {
            dirs.add(path)
        }

        @Override
        void onDirectoryLeave() {
            leftDirs++
        }

        @Override
        void onLeaf(Path path, String value) {
            files[path] = value
        }
    }
}
