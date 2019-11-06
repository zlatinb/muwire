package com.muwire.core

import org.junit.Test

class SplitPatternTest {
    
    @Test
    void testReplaceCharacters()  {
        assert SplitPattern.termify("a_b.c") == ['a','b','c']
    }
    
    @Test
    void testPhrase() {
        assert SplitPattern.termify('"siamese cat"') == ['siamese cat']
    }
    
    @Test
    void testInvalidPhrase() {
        assert SplitPattern.termify('"siamese cat') == ['siamese', 'cat']
    }
    
    @Test
    void testManyPhrases() {
        assert SplitPattern.termify('"siamese cat" any cat "persian cat"') ==
            ['siamese cat','any','cat','persian cat']
    }
    
    @Test
    void testNewLine() {
        def s = "first\nsecond"
        s = s.replaceAll(SplitPattern.SPLIT_PATTERN, " ")
        s = s.split(" ")
        assert s.length == 2
    }
}
