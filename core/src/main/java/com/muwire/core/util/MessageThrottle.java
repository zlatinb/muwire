package com.muwire.core.util;

public class MessageThrottle {
    
    private final long interval;
    private final int allowed;
    
    private final long[] timestamps;
    private long lastMsg;
    private int idx;
    
    public MessageThrottle(long interval, int allowed) {
        this.interval = interval;
        this.allowed = allowed;
        this.timestamps = new long[allowed];
    }
    
    long lastMsg() {
        return lastMsg;
    }
    
    public boolean allow(long now) {
        lastMsg = now;
        final long previous = timestamps[idx];
        if (previous == 0 || now - previous > interval) {
            timestamps[idx++] = now;
            if (idx == allowed)
                idx = 0;
            return true;
        }
        return false;
    }
}
