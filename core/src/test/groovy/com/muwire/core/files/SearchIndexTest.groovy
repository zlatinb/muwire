package com.muwire.core.files

import org.junit.Test

class SearchIndexTest {

	SearchIndex index
	
	private void initIndex(List<String> entries) {
		index = new SearchIndex()
		entries.each {
			File f = new File(it)
			index.add(f)
		}
	}
	
	@Test
	void testSingleTerm() {
		initIndex(["a b.c", "d e.f"])
		
		def found = index.search(["a"])
		assert found.size() == 1
		assert found.contains(new File("a b.c"))
	}
	
	@Test
	void testSingleTermOverlap() {
		initIndex(["a b.c", "c d.e"])
		
		def found = index.search(["c"])
		assert found.size() == 2
		assert found.contains(new File("a b.c"))
		assert found.contains(new File("c d.e"))
	}
	
	@Test
	void testDrillDown() {
		initIndex(["a b.c", "c d.e"])
		
		def found = index.search(["c", "e"])
		assert found.size() == 1
		assert found.contains(new File("c d.e"))
	}
}
