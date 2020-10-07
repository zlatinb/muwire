package com.muwire.core.util;

import java.util.Arrays;

/**
 * A bandwidth counter that uses SMA.
 * @author zab
 *
 */
public class BandwidthCounter {

    private final int memory;
    private final int[] data;
    private final long[] timestamps;
    
    private int index;
    
    public BandwidthCounter(int memory) {
        this.memory = memory;
        this.data = new int[memory];
        this.timestamps = new long[memory];
        Arrays.fill(timestamps, System.currentTimeMillis());   
    }
    
    public int getMemory() {
        return memory;
    }
    
    public void read(int read) {
        if (memory < 1)
            throw new IllegalStateException();
        
        data[index] = read;
        timestamps[index] = System.currentTimeMillis();
        
        index++;
        if (index == memory)
            index = 0;
    }
    
    private int lastIndex() {
        if (index == 0)
            return memory - 1;
        return index - 1;
    }
    
    public int average() {
        int total = 0;
        for (int read : data)
            total += read;
        long delta = timestamps[lastIndex()] - timestamps[index];
        if (delta == 0)
            delta = 1;
        return (int) (total * 1000.0 / delta);
    }

}
