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
    int certificates
    boolean chat

    @Override
    public String toString() {
        super.toString() + "name:$name size:$size sender:${sender.getHumanReadableName()} pieceSize $pieceSize"
    }
}
