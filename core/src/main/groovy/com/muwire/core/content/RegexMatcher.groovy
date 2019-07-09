package com.muwire.core.content

import java.util.regex.Pattern
import java.util.stream.Collectors

class RegexMatcher extends Matcher {
    private final Pattern pattern
    RegexMatcher(String pattern) {
        this.pattern = Pattern.compile(pattern)
    }
    
    @Override
    protected boolean match(List<String> keywords) {
        String combined = keywords.join(" ")
        return pattern.matcher(combined).find()
    }
    
    @Override
    public String getTerm() {
        pattern.pattern()
    }
    
    @Override
    public int hashCode() {
        pattern.pattern().hashCode()
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RegexMatcher))
            return false
        RegexMatcher other = (RegexMatcher) o
        pattern.pattern() == other.pattern.pattern()
    }
}
