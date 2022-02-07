package com.muwire.core.util;

import com.muwire.core.Persona;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SenderThrottle {
    private final long interval;
    private final int allowed;
    
    private final Map<Persona, MessageThrottle> throttleMap =
            new HashMap<>();
    
    public SenderThrottle(long interval, int allowed) {
        this.interval = interval;
        this.allowed = allowed;
    }
    
    public boolean allow(long now, Persona sender) {
        MessageThrottle throttle = throttleMap.computeIfAbsent(sender,
                p -> {return new MessageThrottle(interval, allowed);});
        return throttle.allow(now);
    }
    
    public int clear(long now) {
        int rv = 0;
        for (Iterator<Map.Entry<Persona, MessageThrottle>> iterator = throttleMap.entrySet().iterator();
             iterator.hasNext();) {
            Map.Entry<Persona, MessageThrottle> entry = iterator.next();
            if (now - entry.getValue().lastMsg() > interval) {
                iterator.remove();
                rv++;
            }
        }
        return rv;
    }
}
