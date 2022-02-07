package com.muwire.core.util

import com.muwire.core.Personas
import org.junit.Test

class SenderThrottleTest {
    
    
    private static Personas personas = new Personas()
    
    @Test
    public void testSenders() {
        def throttle = new SenderThrottle(100, 1)
        assert throttle.allow(1, personas.persona1)
        assert throttle.allow(1, personas.persona2)
        
        assert !throttle.allow(2, personas.persona1)
        assert !throttle.allow(2, personas.persona2)
    }
    
    @Test
    public void testClear() {
        def throttle = new SenderThrottle(100, 1)
        
        assert throttle.clear(1) == 0
        
        throttle.allow(1, personas.persona1)
        
        assert throttle.clear(1) == 0
        assert throttle.clear( 102) == 1
    }
}
