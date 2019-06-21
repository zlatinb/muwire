package com.muwire.core.mesh

import com.muwire.core.Constants
import com.muwire.core.InfoHash
import com.muwire.core.download.Pieces
import com.muwire.core.download.SourceDiscoveredEvent
import com.muwire.core.files.FileManager

class MeshManager {
    
    private final Map<InfoHash, Mesh> meshes = Collections.synchronizedMap(new HashMap<>())
    private final FileManager fileManager
    
    MeshManager(FileManager fileManager) {
        this.fileManager = fileManager
    }
    
    Mesh get(InfoHash infoHash) {
        meshes.get(infoHash)
    }
    
    Mesh getOrCreate(InfoHash infoHash, int nPieces) {
        synchronized(meshes) {
            if (meshes.containsKey(infoHash))
                return meshes.get(infoHash)
            Pieces pieces = new Pieces(nPieces, Constants.DOWNLOAD_SEQUENTIAL_RATIO)
            if (fileManager.rootToFiles.containsKey(infoHash)) {
                for (int i = 0; i < nPieces; i++)
                    pieces.markDownloaded(i)
            }
            Mesh rv = new Mesh(infoHash, pieces)
            meshes.put(infoHash, rv)
            return rv
        }
    }
    
    void onSourceDiscoveredEvent(SourceDiscoveredEvent e) {
        Mesh mesh = meshes.get(e.infoHash)
        if (mesh == null)
            return
        mesh.sources.add(e.source.destination)
    }
}
