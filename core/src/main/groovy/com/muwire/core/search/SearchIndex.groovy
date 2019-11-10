package com.muwire.core.search

import com.muwire.core.SplitPattern

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

    private static String[] split(final String source) {
        // first split by split pattern
        String sourceSplit = source.replaceAll(SplitPattern.SPLIT_PATTERN, " ").toLowerCase()
        String [] split = sourceSplit.split(" ")
        def rv = []
        split.each { if (it.length() > 0) rv << it }
        
        // then just by ' '
        source.toLowerCase().split(' ').each { if (it.length() > 0) rv << it }
        
        // and add original string
        rv << source
        rv << source.toLowerCase()
        rv.toArray(new String[0])
    }

    String[] search(List<String> terms) {
        Set<String> rv = null;

        Set<String> powerSet = new HashSet<>()
        terms.each {
            powerSet.addAll(it.toLowerCase().split(' '))
        }
        
        powerSet.each {
            Set<String> forWord = keywords.getOrDefault(it,[])
            if (rv == null) {
                rv = new HashSet<>(forWord)
            } else {
                rv.retainAll(forWord)
            }
        }
        
        // now, filter by terms
        for (Iterator<String> iter = rv.iterator(); iter.hasNext();) {
            String candidate = iter.next()
            candidate = candidate.toLowerCase()
            boolean keep = true
            terms.each { 
                keep &= candidate.contains(it)
            }
            if (!keep)
                iter.remove()
        }

        if (rv != null)
            return rv.asList()
        []
    }
}
