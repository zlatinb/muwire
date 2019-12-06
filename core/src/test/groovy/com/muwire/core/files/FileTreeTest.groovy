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
}
