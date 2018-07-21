package com.muwire.core.files


class SearchIndex {

	final Map<String, Set<File>> keywords = new HashMap<>()
	
	void add(File f) {
		String name = f.getName()
		name = name.replaceAll("\\."," ")
		String [] split = name.split(" ")
		split.each {
			Set<File> existing = keywords.get(it)
			if (existing == null) {
				existing = new HashSet<>()
				keywords.put(it, existing)
			}
			existing.add(f)
		}
	}
	
	File[] search(List<String> terms) {
		Set<File> rv = null;
		
		terms.each {
			Set<File> forWord = keywords.get it
			if (rv == null) {
				rv = forWord
			} else {
				rv.retainAll(forWord)
			}
				
		}
		
		rv.asList()
	}
}
