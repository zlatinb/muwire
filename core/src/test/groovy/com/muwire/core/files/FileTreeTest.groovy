package com.muwire.core.files

import org.junit.After
import org.junit.Before
import org.junit.Test

class FileTreeTest {

    @Before
    @After
    public void cleanup() {
        File a = new File("a")
        File b = new File("b")
        [a,b].each {
            if (it.exists()) {
                if (it.isDirectory())
                    it.deleteDir()
                else
                    it.delete()
            }
        }
    }

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
    
    @Test
    public void testList() {
        Set<File> directories = new HashSet<>()
        Set<String> values = new HashSet<>()
        def cb = new FileListCallback<String>() {

            @Override
            public void onDirectory(File file) {
                directories.add(file)
            }

            @Override
            public void onFile(File file, String value) {
                values.add(value)
            }
        }
        
        File a = new File("a")
        a.delete()
        a.createNewFile()
        File b = new File("b")
        b.delete()
        b.mkdir()
        File c = new File(b, "c")
        c.delete()
        c.createNewFile()
        
        FileTree<String> tree = new FileTree<>()
        
        tree.add(a, "a")
        tree.add(b, "b")
        tree.add(c, "c")
        
        tree.list(null, cb)
        
        assert directories.size() == 1
        assert directories.contains(b)
        assert values.size() == 1
        assert values.contains("a")
        
        directories.clear()
        values.clear()
        tree.list(b, cb)
        assert directories.isEmpty()
        assert values.size() == 1
        assert values.contains("c")
    }
    
    @Test
    public void testCommonAncestor() {
        File a = new File("a")
        File b = new File(a,"b")
        File c = new File(b,"c")
        File d = new File(b,"d")
        
        a.delete()
        b.delete()
        c.delete()
        d.delete()
        
        a.mkdir()
        b.mkdir()
        c.createNewFile()
        d.createNewFile()
        
        FileTree<Void> tree = new FileTree<>()
        tree.add(a, null)
        tree.add(b, null)
        tree.add(c, null)
        tree.add(d, null)
        
        File common = tree.commonAncestor()
        
        assert common == b
    }
}
