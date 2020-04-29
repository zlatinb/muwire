package com.muwire.core.mesh

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.download.Pieces

import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

class Mesh {
    private final InfoHash infoHash
    private final Set<Persona> sources = new ConcurrentHashSet<>()
    final Pieces pieces

    Mesh(InfoHash infoHash, Pieces pieces) {
        this.infoHash = infoHash
        this.pieces = pieces
    }

    Set<Persona> getRandom(int n, Persona exclude) {
        List<Persona> tmp = new ArrayList<>(sources)
        tmp.remove(exclude)
        Collections.shuffle(tmp)
        if (tmp.size() < n)
            return tmp
        tmp[0..n-1]
    }
}
