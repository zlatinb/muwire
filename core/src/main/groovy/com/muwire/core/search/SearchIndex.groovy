package com.muwire.core.search

import com.muwire.core.Constants

class SearchIndex {

	final Map<String, Set<String>> keywords = new HashMap<>()
	
	void add(String string) {
		String [] split = split(string)
		split.each {
			Set<String> existing = keywords.get(it)
			if (existing == null) {
				existing = new HashSet<>()
				keywords.put(it, existing)
			}
			existing.add(string)
		}
	}
	
	void remove(String string) {
		String [] split = split(string)
		split.each {
			Set<String> existing = keywords.get it
			if (existing != null) {
				existing.remove(string)
				if (existing.isEmpty()) {
					keywords.remove(it)
				}
			}
		}
	}
	
	private static String[] split(String source) {
		source = source.replaceAll(Constants.SPLIT_PATTERN, " ").toLowerCase()
		source.split(" ")
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
		
        System.out.println("rv is $rv")
        
		if (rv != null)
			return rv.asList()
		[]
	}
}
