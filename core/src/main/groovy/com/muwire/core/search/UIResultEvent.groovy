package com.muwire.core.search

import com.muwire.core.Event
import com.muwire.core.InfoHash
import com.muwire.core.Persona

import net.i2p.data.Destination

class UIResultEvent extends Event {
    Persona sender
    Set<Destination> sources
    UUID uuid
    String name
    long size
    InfoHash infohash
    int pieceSize
    String comment
    boolean browse
    boolean browseCollections
    int certificates
    boolean chat
    boolean feed
    boolean messages
    Set<InfoHash> collections
    String[] path
    
    
    private String fullPath
    String getFullPath() {
        if (fullPath == null) { 
            if (path != null && path.length > 0) {
                List<String> elements = new ArrayList<>()
                for (String element : path)
                    elements << element
                elements.remove(0) // nix hidden root
                elements << name
                fullPath = elements.join(File.separator)
            } else
                fullPath = name
        }
        fullPath
    }
    
    @Override
    public String toString() {
        super.toString() + "name:$name size:$size sender:${sender.getHumanReadableName()} pieceSize $pieceSize"
    }
}
