package com.muwire.core.util

import org.junit.Test

class FixedSizeFIFOSetTest {
    
    @Test
    public void testFifo() {
        FixedSizeFIFOSet<String> fifoSet = new FixedSizeFIFOSet(3);
        fifoSet.add("a")
        assert fifoSet.contains("a")
        
        fifoSet.add("b")
        assert fifoSet.contains("a")
        assert fifoSet.contains("b")
        
        fifoSet.add("c")
        assert fifoSet.contains("a")
        assert fifoSet.contains("b")
        assert fifoSet.contains("c")
        
        fifoSet.add("d")
        assert !fifoSet.contains("a")
        assert fifoSet.contains("b")
        assert fifoSet.contains("c")
        assert fifoSet.contains("d")
    }

    
    @Test
    public void testDuplicateElement() {
        FixedSizeFIFOSet<String> fifoSet = new FixedSizeFIFOSet(3);
        
        fifoSet.add("a")
        fifoSet.add("b")
        fifoSet.add("c")
        fifoSet.add("a")
        
        assert fifoSet.contains("a")
        assert fifoSet.contains("b")
        assert fifoSet.contains("c")
        
        fifoSet.add("d")
        assert fifoSet.contains("a")
        assert !fifoSet.contains("b")
        assert fifoSet.contains("c")
        assert fifoSet.contains("d")
    }
}
