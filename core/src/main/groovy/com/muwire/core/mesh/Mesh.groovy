package com.muwire.core.mesh

import com.muwire.core.InfoHash

import com.muwire.core.download.Pieces

import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

class Mesh {
    private final InfoHash infoHash
    private final Set<Destination> sources = new ConcurrentHashSet<>()
    private final Pieces pieces
    
    Mesh(InfoHash infoHash, Pieces pieces) {
        this.infoHash = infoHash
        this.pieces = pieces
    }
}
