package com.muwire.core.search

import org.junit.Test

class SearchIndexTest {

	SearchIndex index
	
	private void initIndex(List<String> entries) {
		index = new SearchIndex()
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
}
