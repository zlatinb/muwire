package com.muwire.core.util

import org.junit.Test

class MessageThrottleTest {
    
    @Test
    public void testThrottleOne() {
        def throttle = new MessageThrottle(100, 1)
        assert throttle.allow(1)
        assert !throttle.allow(2)
        assert !throttle.allow(10)
        assert throttle.allow(102)
    }
    
    @Test
    public void testAllowThree() {
        def throttle = new MessageThrottle(100, 3)
        assert throttle.allow(1)
        assert throttle.allow(2)
        assert throttle.allow(3)
        
        assert !throttle.allow(101)
        
        assert throttle.allow(102)
    }
}
