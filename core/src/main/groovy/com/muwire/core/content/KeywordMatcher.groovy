package com.muwire.core.content

class KeywordMatcher extends Matcher {
    private final String keyword
    KeywordMatcher(String keyword, MatchAction action, String name) {
        super(action, name)
        this.keyword = keyword
    }
    
    @Override
    protected boolean match(List<String> searchTerms) {
        boolean found = false
        searchTerms.each { 
            if (keyword == it)
                found = true
        }
        found
    }
    
    @Override
    public String getTerm() {
        keyword
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
