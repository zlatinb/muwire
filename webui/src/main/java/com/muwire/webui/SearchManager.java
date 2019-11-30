package com.muwire.webui;

import java.util.HashSet;
import java.util.Set;

import com.muwire.core.Core;

class SearchManager {
    
    
    private final Core core;
    
    private final Set<String> searches = new HashSet<>();
    
    SearchManager(Core core) {
        this.core = core;
    }
    
    void newSearch(String search) {
        searches.add(search);
    }
    
    Iterable<String> getSearches() {
        return searches;
    }

}
