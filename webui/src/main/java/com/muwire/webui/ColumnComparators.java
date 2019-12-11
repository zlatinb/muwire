package com.muwire.webui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ColumnComparators<T> {
    
    private final Map<String, Comparator<T>> comparators = new HashMap<>();
    
    void add(String key, Comparator<T> comparator) {
        comparators.put(key, comparator);
    }
    
    Comparator<T> get(String key, String order) {
        Comparator<T> rv = comparators.get(key);
        if (rv != null && order.equals("ascending"))
            rv = rv.reversed();
        return rv;
    }

}
