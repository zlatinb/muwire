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
    
    @Override
    public int hashCode() {
        keyword.hashCode()
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof KeywordMatcher))
            return false
        KeywordMatcher other = (KeywordMatcher) o
        keyword.equals(other.keyword)
    }
}
