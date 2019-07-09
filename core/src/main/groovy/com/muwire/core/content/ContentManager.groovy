package com.muwire.core.content

import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.search.QueryEvent

import net.i2p.util.ConcurrentHashSet

class ContentManager {

    Set<Matcher> matchers = new ConcurrentHashSet()    
    
    void onContentControlEvent(ContentControlEvent e) {
        Matcher m
        if (e.regex)
            m = new RegexMatcher(e.term)
        else
            m = new KeywordMatcher(e.term)
        if (e.add)
            matchers.add(m)
        else
            matchers.remove(m)
    }
    
    void onQueryEvent(QueryEvent e) {
        if (e.searchEvent.searchTerms == null)
            return
        matchers.each { it.process(e) }
    }
}
