package com.muwire.core.content

class KeywordMatcher extends Matcher {
    private final String keyword
    KeywordMatcher(String keyword) {
        this.keyword = keyword
    }
    
    @Override
    protected boolean match(String[] searchTerms) {
        searchTerms.each { 
            if (keyword == it)
                return true
        }
        false
    }
}
