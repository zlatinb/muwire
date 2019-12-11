package com.muwire.webui;

import java.util.Arrays;
import java.util.Collections;
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

import net.i2p.data.Destination;
import net.i2p.util.ConcurrentHashSet;

public class SearchResults {
    
    private final UUID uuid;
    private final String search;
    private final Map<Persona, Set<UIResultEvent>> bySender = new ConcurrentHashMap<>();
    private final Map<InfoHash, Set<UIResultEvent>> byInfohash = new ConcurrentHashMap<>();
    private final Map<InfoHash, Set<Destination>> possibleSources = new ConcurrentHashMap<>();
    private volatile long revision;
    
    public SearchResults(UUID uuid, String search) {
        this.uuid = uuid;
        this.search = search;
    }
    
    long getRevision() {
        return revision;
    }
    
    void addResults(UIResultBatchEvent e) {
        revision++;
        Persona sender = e.getResults()[0].getSender();
        Set<UIResultEvent> existing = bySender.get(sender);
        if (existing == null) {
            existing = new ConcurrentHashSet<>();
            bySender.put(sender, existing);
        }
        existing.addAll(Arrays.asList(e.getResults()));
        
        for(UIResultEvent result : e.getResults()) {
            existing = byInfohash.get(result.getInfohash());
            if (existing == null) {
                existing = new ConcurrentHashSet<>();
                byInfohash.put(result.getInfohash(), existing);
            }
            existing.add(result);
            
            Set<Destination> sources = possibleSources.get(result.getInfohash());
            if (sources == null) {
                sources = new ConcurrentHashSet<>();
                possibleSources.put(result.getInfohash(), sources);
            }
            sources.addAll(result.getSources());
        }
    }
    
    int getResultCount() {
        int total = 0;
        for (Set<UIResultEvent> set : bySender.values())
            total += set.size();
        return total;
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
    
    public Map<InfoHash, Set<UIResultEvent>> getByInfoHash() {
        return byInfohash;
    }
    
    public Set<UIResultEvent> getByInfoHash(InfoHash infoHash) {
        return byInfohash.get(infoHash);
    }
    
    public Set<Destination> getPossibleSources(InfoHash infoHash) {
        return possibleSources.getOrDefault(infoHash, Collections.emptySet());
    }
    
    int getSenderCount() {
        return bySender.size();
    }
    
    int totalResults() {
        int total = 0;
        for(Set<UIResultEvent> results : bySender.values())
            total += results.size();
        return total;
    }

}
