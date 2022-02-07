package com.muwire.webui;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.muwire.core.Core;
import com.muwire.core.SplitPattern;
import com.muwire.core.search.QueryEvent;
import com.muwire.core.search.SearchEvent;
import com.muwire.core.search.UIResultBatchEvent;
import com.muwire.core.util.DataUtil;

import net.i2p.crypto.DSAEngine;
import net.i2p.data.Base64;
import net.i2p.data.Signature;

class SearchManager {
    
    static final UUID INVALID_REGEX_UUID = UUID.randomUUID();
    
    private final Core core;
    
    private final Map<UUID, SearchResults> results = new HashMap<>();
    
    SearchManager(Core core) {
        this.core = core;
    }
    
    UUID newSearch(String search) {
        search = search.trim();
        if (search.length() == 0)
            return null;
        else {
            UUID uuid = UUID.randomUUID();
            
            
            boolean hashSearch = false;
            byte [] root = null;
            if (search.length() == 44 && search.indexOf(' ') < 0) {
                try {
                    root = Base64.decode(search);
                    hashSearch = true;
                } catch (Exception e) {
                    // not a hash search
                }
            }
            
            boolean regexSearch = false;
            if (search.length() > 1 && search.startsWith("/") && search.endsWith("/")) {
                search = search.substring(1, search.length() - 1);
                try {
                    Pattern.compile(search);
                    regexSearch = true;
                } catch (Exception e) {
                    // not a valid regex
                    return INVALID_REGEX_UUID;
                }
            }
            
            SearchResults searchResults = new SearchResults(uuid, search);
            results.put(uuid, searchResults);
            SearchEvent searchEvent = new SearchEvent();
            searchEvent.setOobInfohash(true);
            searchEvent.setCompressedResults(true);
            searchEvent.setPersona(core.getMe());
            searchEvent.setUuid(uuid);
            searchEvent.setRegex(regexSearch);
            byte[] payload;
            if (hashSearch) {
                searchEvent.setSearchHash(root);
                payload = root;
            } else {
                String[] nonEmpty;
                if (regexSearch) {
                    nonEmpty = new String[] { search };
                    payload = search.getBytes(StandardCharsets.UTF_8);
                } else {
                    nonEmpty = SplitPattern.termify(search);
                    payload = String.join(" ", nonEmpty).getBytes(StandardCharsets.UTF_8);
                }
                searchEvent.setSearchTerms(Arrays.asList(nonEmpty));
                searchEvent.setSearchComments(core.getMuOptions().getSearchComments());
                searchEvent.setCollections(core.getMuOptions().getSearchCollections());
                searchEvent.setSearchPaths(core.getMuOptions().getSearchPaths());
            }
            
            boolean firstHop = core.getMuOptions().allowUntrusted() || core.getMuOptions().getSearchExtraHop();
            
            Signature sig = DSAEngine.getInstance().sign(payload, core.getSpk());
            
            long timestamp = System.currentTimeMillis();
            QueryEvent queryEvent = new QueryEvent();
            queryEvent.setSearchEvent(searchEvent);
            queryEvent.setFirstHop(firstHop);
            queryEvent.setLocal(true);
            queryEvent.setReplyTo(core.getMe().getDestination());
            queryEvent.setReceivedOn(core.getMe().getDestination());
            queryEvent.setOriginator(core.getMe());
            queryEvent.setSig(sig.getData());
            queryEvent.setQueryTime(timestamp);
            queryEvent.setSig2(DataUtil.signUUID(uuid, timestamp, core.getSpk()));
            
            core.getEventBus().publish(queryEvent);
            return uuid;
        }
    }
    
    public void stopSearch(UUID uuid) {
        results.remove(uuid);
    }
    
    public Map<UUID,SearchResults> getResults() {
        return results;
    }

    public void onUIResultBatchEvent(UIResultBatchEvent e) {
        UUID uuid = e.getResults()[0].getUuid();
        SearchResults searchResults = results.get(uuid);
        if (searchResults == null)
            return; // oh well
        searchResults.addResults(e);
    }
}
