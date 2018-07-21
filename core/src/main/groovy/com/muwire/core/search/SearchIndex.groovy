package com.muwire.core.search


class SearchIndex {

	final Map<String, Set<String>> keywords = new HashMap<>()
	
	void add(String string) {
		String name = string.replaceAll("\\."," ")
		String [] split = name.split(" ")
		split.each {
			Set<String> existing = keywords.get(it)
			if (existing == null) {
				existing = new HashSet<>()
				keywords.put(it, existing)
			}
			existing.add(string)
		}
	}
	
	String[] search(List<String> terms) {
		Set<String> rv = null;
		
		terms.each {
			Set<String> forWord = keywords.getOrDefault(it,[])
			if (rv == null) {
				rv = forWord
			} else {
				rv.retainAll(forWord)
			}
				
		}
		
		if (rv != null)
			return rv.asList()
		[]
	}
}
