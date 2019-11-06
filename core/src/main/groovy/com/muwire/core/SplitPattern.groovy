package com.muwire.core

class SplitPattern {
    
    public static final String SPLIT_PATTERN = "[\\*\\+\\-,\\.:;\\(\\)=_/\\\\\\!\\\"\\\'\\\$%\\|\\[\\]\\{\\}\\?\r\n]";
 
    private static final Set<Character> SPLIT_CHARS = new HashSet<>()
    static {
        SPLIT_CHARS.with { 
            add(' '.toCharacter())
            add('*'.toCharacter())
            add('+'.toCharacter())
            add('-'.toCharacter())
            add(','.toCharacter())
            add('.'.toCharacter())
            add(':'.toCharacter())
            add(';'.toCharacter())
            add('('.toCharacter())
            add(')'.toCharacter())
            add('='.toCharacter())
            add('_'.toCharacter())
            add('/'.toCharacter())
            add('\\'.toCharacter())
            add('!'.toCharacter())
            add('\''.toCharacter())
            add('$'.toCharacter())
            add('%'.toCharacter())
            add('|'.toCharacter())
            add('['.toCharacter())
            add(']'.toCharacter())
            add('{'.toCharacter())
            add('}'.toCharacter())
            add('?'.toCharacter())
        }
    }
    
    public static String[] termify(final String source) {
        String lowercase = source.toLowerCase().trim()
        
        def rv = []
        int pos = 0
        int quote = -1
        
        StringBuilder tmp = new StringBuilder()
        while(pos < lowercase.length()) {
            char c = lowercase.charAt(pos++)
            if (quote < 0 && c == '"') {
                quote = pos - 1
                continue
            }
            if (quote >= 0) {
                if (c == '"') {
                    quote = -1
                    if (tmp.length() != 0) {
                        rv << tmp.toString()
                        tmp = new StringBuilder()
                    }
                } else
                    tmp.append(c)
            } else if (SPLIT_CHARS.contains(c)) {
                if (tmp.length() != 0) {
                    rv << tmp.toString()
                    tmp = new StringBuilder()
                }
            } else
                tmp.append c
        }
        
        // check if odd number of quotes and re-tokenize from last quote
        if (quote >= 0) {
            tmp = new StringBuilder()
            pos = quote + 1
            while(pos < lowercase.length()) {
                char c = lowercase.charAt(pos++)
                if (SPLIT_CHARS.contains(c)) {
                    if (tmp.length() > 0) {
                        rv << tmp.toString()
                        tmp = new StringBuilder()
                    }
                } else
                    tmp.append(c)
            }
        }
        
        if (tmp.length() > 0)
            rv << tmp.toString()
        
        rv
    }   
    
}
