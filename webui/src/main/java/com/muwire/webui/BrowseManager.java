package com.muwire.webui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

import com.muwire.core.Core;
import com.muwire.core.Persona;
import com.muwire.core.search.BrowseStatus;
import com.muwire.core.search.BrowseStatusEvent;
import com.muwire.core.search.UIBrowseEvent;
import com.muwire.core.search.UIResultBatchEvent;
import com.muwire.core.search.UIResultEvent;

public class BrowseManager {

    private final Core core;
    private final Map<Persona, Browse> browses = new ConcurrentHashMap<>();
    
    public BrowseManager(Core core) {
        this.core = core;
    }

    public void onBrowseStatusEvent(BrowseStatusEvent e) {
        Browse browse = browses.get(e.getHost());
        if (browse == null)
            return; // hmm
        browse.status = e.getStatus();
        browse.revision++;
        if (browse.status == BrowseStatus.FETCHING) {
            browse.totalResults = e.getTotalResults();
            browse.uuid = e.getUuid();
        }
    }
    
    public void onUIResultBatchEvent(UIResultBatchEvent e) {
        Browse browse = browses.get(e.getResults()[0].getSender());
        if (browse == null)
            return;
        if (!browse.uuid.equals(e.getUuid()))
            return;
        browse.results.addAll(Arrays.asList(e.getResults()));
        browse.revision++;
    }
    
    void browse(Persona p) {
        Browse browse = new Browse(p);
        browses.put(p, browse);
        UIBrowseEvent event = new UIBrowseEvent();
        event.setHost(p);
        core.getEventBus().publish(event);
    }
    
    boolean isBrowsing(Persona p) {
        Browse browse = browses.get(p);
        if (browse == null)
            return false;
        return browse.status == BrowseStatus.CONNECTING || 
                browse.status == BrowseStatus.FETCHING;
    }
    
    Map<Persona, Browse> getBrowses(){
        return browses;
    }
    
    static class Browse {
        private final Persona persona;
        private volatile BrowseStatus status;
        private volatile int totalResults;
        private volatile long revision;
        private volatile UUID uuid;
        private final List<UIResultEvent> results = Collections.synchronizedList(new ArrayList<>());
        
        Browse(Persona persona) {
            this.persona = persona;
        }

        public BrowseStatus getStatus() {
            return status;
        }

        public int getTotalResults() {
            return totalResults;
        }

        public List<UIResultEvent> getResults() {
            return results;
        }
        
        public long getRevision() {
            return revision;
        }
        
        public Persona getHost() {
            return persona;
        }
    }
}
