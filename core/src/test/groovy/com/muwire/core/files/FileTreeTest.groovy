package com.muwire.core.files

import org.junit.Test

class FileTreeTest {
    
    @Test
    public void testRemoveEmtpyDirs() {
        File a = new File("a")
        File b = new File(a, "b")
        File c = new File(b, "c")
        
        FileTree<Void> tree = new FileTree<>()
        tree.add(c,null)
        
        assert tree.root.children.size() == 1
        assert tree.fileToNode.size() == 3
        
        tree.remove(b)
        assert tree.root.children.size() == 0
        assert tree.fileToNode.isEmpty()
    }
    
    @Test
    public void testRemoveFileFromNonEmptyDir() {
        File a = new File("a")
        File b = new File(a,"b")
        File c = new File(b, "c")
        File d = new File(b, "d")
        
        FileTree<Void> tree = new FileTree<>()
        tree.add(c,null)
        
        assert tree.fileToNode.size() == 3
        
        tree.add(d, null)
        assert tree.fileToNode.size() == 4
        
        tree.remove(d)
        assert tree.fileToNode.size() == 3
    }
    
    @Test
    public void testTraverse() {
        Stack stack = new Stack()
        Set<String> values = new HashSet<>()
        StringBuilder sb = new StringBuilder()
        def cb = new FileTreeCallback<String>() {

            @Override
            public void onDirectoryEnter(File file) {
                stack.push(file)
            }

            @Override
            public void onDirectoryLeave() {
                stack.pop()
            }

            @Override
            public void onFile(File file, String value) {
                values.add(value)
            }
        }
        
        File a = new File("a")
        a.createNewFile()
        File b = new File("b")
        b.mkdir()
        File c = new File(b, "c")
        c.createNewFile()
        File d = new File(b, "d")
        d.mkdir()
        File e = new File(d, "e")
        e.createNewFile()
        FileTree<String> tree = new FileTree<>()
        
        tree.add(a, "a")
        tree.add(b, "b")
        tree.add(c, "c")
        tree.add(d, "d")
        tree.add(e, "e")
        
        tree.traverse(cb)

        assert stack.isEmpty()
        assert values.size() == 3
        assert values.contains("a")
        assert values.contains("c")
        assert values.contains("e")
    }
}
