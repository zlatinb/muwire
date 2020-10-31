package com.muwire.core.collections

import java.nio.file.Path
import java.nio.file.Paths

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.SharedFile
import com.muwire.core.files.FileTree

import net.i2p.data.SigningPrivateKey

class FileCollectionBuilder {
    private long timestamp
    private String root
    private Persona author
    private String comment = ""
    private final Set<SharedFile> sharedFiles = new LinkedHashSet<>()
    private SigningPrivateKey spk

    public FileCollectionBuilder setRoot(String root) {
        this.root = root
        this
    }
    
    public FileCollectionBuilder setTimestamp(long timestamp) {
        this.timestamp = timestamp
        this
    }
    
    public FileCollectionBuilder setAuthor(Persona author) {
        this.author = author
        this
    }
    
    public FileCollectionBuilder setComment(String comment) {
        this.comment = comment
        this
    }
    
    public FileCollectionBuilder setSPK(SigningPrivateKey spk) {
        this.spk = spk
        this
    }
    
    public FileCollectionBuilder addFile(SharedFile sf) {
        this.sharedFiles.add(sf)
        this
    }
        
    public FileCollection build() {
        // TODO: check constraints
        if (root == null || timestamp == 0 || author == null || comment == null || spk == null || sharedFiles.isEmpty())
            throw new InvalidCollectionException()
            
        FileTree<Void> tree = new FileTree<>()
        for(SharedFile sf : sharedFiles) 
            tree.add(sf.file, null)
        File common = tree.commonAncestor()

        Map<SharedFile, Path> sfPaths = new LinkedHashMap<>()
        for(SharedFile sf : sharedFiles) {
            Path path = sf.file.toPath()
            sfPaths.put(sf, path)
        }
        
        if (common != null) {
            Path commonPath = common.toPath()
            for (SharedFile sf : sfPaths.keySet()) 
                sfPaths.put(sf, commonPath.relativize(sfPaths.get(sf)))
        }
        
        Map<SharedFile, LinkedList<String>> sfPathElements = new HashMap<>()
        for (SharedFile sf : sfPaths.keySet()) {
            LinkedList elements = new LinkedList()
            for (Path p : sfPaths.get(sf))
                elements.add(p.toString())
            elements.addFirst(root)
            sfPathElements.put(sf, elements)
        }
            
        Set<FileCollectionItem> files = new LinkedHashSet<>()
        for (SharedFile sf : sfPathElements.keySet()) {
            String comment = sf.getComment() // TODO: check comment encoding
            if (comment == null)
                comment = ""
            def item = new FileCollectionItem(new InfoHash(sf.root), comment, sfPathElements.get(sf), (byte)sf.pieceSize, sf.getCachedLength())
            files.add(item)
        } 
        
        def rv = new FileCollection(timestamp, author, comment, files, spk)
        rv
    }
    

}
