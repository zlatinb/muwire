package com.muwire.core.search

import com.muwire.core.SplitPattern

/**
 * This is split into a Groovy part and Java part.
 * The Groovy part is needed because Java regexes do not support
 * the full split string.
 * The Java part is needed for speed.
 */
class SearchIndex {

    private boolean closed
    private final SearchIndexImpl actualSearchIndex
    
    SearchIndex(File dir, String name) {
        actualSearchIndex = new SearchIndexImpl(dir, name)
    }
    
    synchronized void add(String string) {
        if (closed)
            return
        actualSearchIndex.add(string, split(string))
    }

    synchronized void remove(String string) {
        if (closed)
            return
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

    synchronized String[] search(List<String> terms) {
        if (closed)
            return new String[0]
        return actualSearchIndex.search(terms)
    }
    
    synchronized void close() {
        if (closed)
            return
        closed = true
        actualSearchIndex.close()
    }

}
