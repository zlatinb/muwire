package com.muwire.core.mesh

import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.download.Pieces
import com.muwire.core.util.DataUtil

import net.i2p.data.Base64
import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

/**
 * Representation of a download mesh.
 * 
 * Two data structures - collection of known sources and collection of sources
 * we have successfully transferred data with.
 * 
 * @author zab
 */
class Mesh {
    private final InfoHash infoHash
    private final Map<Destination,Persona> sources = new HashMap<>()
    private final Set<Destination> verified = new HashSet<>()
    final Pieces pieces

    Mesh(InfoHash infoHash, Pieces pieces) {
        this.infoHash = infoHash
        this.pieces = pieces
    }

    synchronized Set<Persona> getRandom(int n, Persona exclude) {
        List<Destination> tmp = new ArrayList<>(verified)
        if (exclude != null)
            tmp.remove(exclude.destination)
        tmp.retainAll(sources.keySet()) // verified may contain nodes not in sources
        Collections.shuffle(tmp)
        if (tmp.size() > n)
            tmp = tmp[0..n-1]
        tmp.collect(new HashSet<>(), { sources[it] })
    }
    
    synchronized void add(Persona persona) {
        sources.put(persona.destination, persona)
    }
    
    synchronized void verify(Destination d) {
        verified.add(d)
    }
    
    synchronized def toJson() {
        def json = [:]
        json.timestamp = System.currentTimeMillis()
        json.infoHash = Base64.encode(infoHash.getRoot())
        
        Set<Persona> toPersist = new HashSet<>(sources.values())
        toPersist.retainAll { verified.contains(it.destination) }
        json.sources = toPersist.collect {it.toBase64()}
        json.nPieces = pieces.nPieces
        List<Integer> downloaded = pieces.getDownloaded()
        if( downloaded.size() > pieces.nPieces)
            return null
        json.xHave = DataUtil.encodeXHave(downloaded, pieces.nPieces)
        json
    }
}
