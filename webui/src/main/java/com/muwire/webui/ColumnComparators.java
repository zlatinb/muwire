package com.muwire.webui;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class ColumnComparators<T> {
    
    private final Map<String, Comparator<T>> comparators = new HashMap<>();
    
    void add(String key, Comparator<T> comparator) {
        comparators.put(key, comparator);
    }
    
    private Comparator<T> get(String key, String order) {
        Comparator<T> rv = comparators.get(key);
        if (rv != null && order.equals("ascending"))
            rv = rv.reversed();
        return rv;
    }

    void sort(List<T> items, HttpServletRequest req) {
        String key = req.getParameter("key");
        String order = req.getParameter("order");
        Comparator<T> comparator = get(key, order);
        if (comparator != null)
            Collections.sort(items, comparator);
    }
}
