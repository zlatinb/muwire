package com.muwire.core.search

import org.junit.Test

class SearchIndexTest {

    SearchIndex index

    private void initIndex(List<String> entries) {
        index = new SearchIndex("testIndex")
        entries.each {
            index.add(it)
        }
    }

    @Test
    void testSingleTerm() {
        initIndex(["a b.c", "d e.f"])

        def found = index.search(["a"])
        assert found.size() == 1
        assert found.contains("a b.c")
    }

    @Test
    void testSingleTermOverlap() {
        initIndex(["a b.c", "c d.e"])

        def found = index.search(["c"])
        assert found.size() == 2
        assert found.contains("a b.c")
        assert found.contains("c d.e")

    }

    @Test
    public void testDrillDownDoesNotModifyIndex() {
        initIndex(["a b.c", "c d.e"])
        index.search(["c","e"])
        def found = index.search(["c"])
        assert found.size() == 2
        assert found.contains("a b.c")
        assert found.contains("c d.e")
    }

    @Test
    void testDrillDown() {
        initIndex(["a b.c", "c d.e"])

        def found = index.search(["c", "e"])
        assert found.size() == 1
        assert found.contains("c d.e")
    }

    @Test
    void testNotFound() {
        initIndex(["a b.c"])
        def found = index.search(["d"])
        assert found.size() == 0
    }

    @Test
    void testSomeNotFound() {
        initIndex(["a b.c"])
        def found = index.search(["a","d"])
        assert found.size() == 0

    }

    @Test
    void testRemove() {
        initIndex(["a b.c"])
        index.remove("a b.c")
        def found = index.search(["a"])
        assert found.size() == 0
    }

    @Test
    void testRemoveOverlap() {
        initIndex(["a b.c", "b c.d"])
        index.remove("a b.c")
        def found = index.search(["b"])
        assert found.size() == 1
        assert found.contains("b c.d")
    }

    @Test
    void testDuplicateTerm() {
        initIndex(["MuWire-0.3.3.jar"])
        def found = index.search(["muwire", "0", "3", "jar"])
        assert found.size() == 1
    }
    
    @Test
    void testOriginalText() {
        initIndex(["a-b c-d"])
        def found = index.search(['a-b'])
        assert found.size() == 1
        found = index.search(['c-d'])
        assert found.size() == 1
    }
    
    @Test
    void testPhrase() {
        initIndex(["a-b c-d e-f"])
        def found = index.search(['a-b c-d'])
        assert found.size() == 1
        assert index.search(['c-d e-f']).size() == 1
        assert index.search(['a-b e-f']).size() == 0
    }
    
    @Test
    void testMixedPhraseAndKeyword() {
        initIndex(["My siamese cat video", 
            "My cat video of a siamese", 
            "Video of a siamese cat"])
        
        assert index.search(['cat video']).size() == 2
        assert index.search(['cat video','siamese']).size() == 2
        assert index.search(['cat', 'video siamese']).size() == 0
        assert index.search(['cat','video','siamese']).size() == 3
    }
    
    @Test
    void testNewLine() {
        initIndex(['first\nsecond'])
        assert index.search(['first']).size() == 1
        assert index.search(['second']).size() == 1
        assert index.search(['first','second']).size() == 1
        assert index.search(['second','first']).size() == 1
        assert index.search(['second first']).size() == 0
        assert index.search(['first second']).size() == 0
    }
    
    @Test
    void testDosNewLine() {
        initIndex(['first\r\nsecond'])
        assert index.search(['first']).size() == 1
        assert index.search(['second']).size() == 1
        assert index.search(['first','second']).size() == 1
        assert index.search(['second','first']).size() == 1
        assert index.search(['second first']).size() == 0
        assert index.search(['first second']).size() == 0
    }
}
