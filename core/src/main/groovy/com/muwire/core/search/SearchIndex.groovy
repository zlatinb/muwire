package com.muwire.core.search

import com.muwire.core.SplitPattern

/**
 * This is split into a Groovy part and Java part.
 * The Groovy part is needed because Java regexes do not support
 * the full split string.
 * The Java part is needed for speed.
 */
class SearchIndex {

    
    private final SearchIndexImpl actualSearchIndex
    
    SearchIndex(String name) {
        actualSearchIndex = new SearchIndexImpl(name)
    }
    
    synchronized void add(String string) {
        actualSearchIndex.add(string, split(string))
    }

    synchronized void remove(String string) {
        actualSearchIndex.remove(string, split(string))
    }

    private static String[] split(final String source) {
        // first split by split pattern
        String sourceSplit = source.replaceAll(SplitPattern.SPLIT_PATTERN, " ").toLowerCase()
        String [] split = sourceSplit.split(" ")
        def rv = new HashSet()
        split.each { if (it.length() > 0) rv << it }
        
        // then just by ' '
        source.toLowerCase().split(' ').each { if (it.length() > 0) rv << it }
        
        // and add original string
        rv << source
        rv << source.toLowerCase()
        rv.toArray(new String[0])
    }

    String[] search(List<String> terms) {
        return actualSearchIndex.search(terms)
    }
    

}
