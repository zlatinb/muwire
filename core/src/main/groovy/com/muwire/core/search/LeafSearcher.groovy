package com.muwire.core.search

import com.muwire.core.connection.DisconnectionEvent
import com.muwire.core.connection.UltrapeerConnectionManager

import net.i2p.data.Destination

class LeafSearcher {
    
    final UltrapeerConnectionManager connectionManager
    final SearchIndex searchIndex = new SearchIndex()
    
    final Map<String, Set<byte[]>> fileNameToHashes = new HashMap<>()
    final Map<byte[], Set<Destination>> hashToLeafs = new HashMap<>()
    
    final Map<Destination, Map<byte[], Set<String>>> leafToFiles = new HashMap<>()
    
    LeafSearcher(UltrapeerConnectionManager connectionManager) {
        this.connectionManager = connectionManager
    }
    
    void onUpsertEvent(UpsertEvent e) {
        // TODO: implement
    }
    
    void onDeleteEvent(DeleteEvent e) {
        // TODO: implement
    }
    
    void onDisconnectionEvent(DisconnectionEvent e) {
        // TODO: implement
    }
    
    void onQueryEvent(QueryEvent e) {
        // TODO: implement
    }
    
}
