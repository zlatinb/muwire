package com.muwire.webui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.muwire.core.InfoHash;
import com.muwire.core.Persona;
import com.muwire.core.search.UIResultBatchEvent;
import com.muwire.core.search.UIResultEvent;

public class SearchResults {
    
    private final UUID uuid;
    private final String search;
    private final Map<Persona, Set<UIResultEvent>> bySender = new ConcurrentHashMap<>();
    
    public SearchResults(UUID uuid, String search) {
        this.uuid = uuid;
        this.search = search;
    }
    
    void addResults(UIResultBatchEvent e) {
        Persona sender = e.getResults()[0].getSender();
        Set<UIResultEvent> existing = bySender.get(sender);
        if (existing == null) {
            existing = new HashSet<>();
            bySender.put(sender, existing);
        }
        existing.addAll(Arrays.asList(e.getResults()));
    }
    
    public UUID getUUID() {
        return uuid;
    }
    
    public String getSearch() {
        return search;
    }
    
    public Map<Persona, Set<UIResultEvent>> getBySender() {
        return bySender;
    }
    
    public Set<UIResultEvent> getByInfoHash(InfoHash infoHash) {
        return bySender.values().stream().
            flatMap(r -> r.stream()).
            filter(r -> r.getInfohash().equals(infoHash)).
            collect(Collectors.toSet());
    }

}
