package com.muwire.core.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class FixedSizeFIFOSet<T> {

    private final int capacity;
    private final Set<T> set = new HashSet<>();
    private final Deque<T> fifo = new ArrayDeque<>();
    
    public FixedSizeFIFOSet(final int capacity) {
        this.capacity = capacity;
    }

    public boolean contains(T element) {
        return set.contains(element);
    }
    
    public void add(T element) {
        if (!set.contains(element)) {
            if (set.size() == capacity) {
                T toRemove = fifo.removeLast();
                set.remove(toRemove);
            }
            fifo.addFirst(element);
            set.add(element);
        } else {
            fifo.remove(element);
            fifo.addFirst(element);
        }
    }
}
