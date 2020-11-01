package com.muwire.core.collections

import com.muwire.core.Constants
import com.muwire.core.InfoHash
import com.muwire.core.Name
import com.muwire.core.files.FileHasher

class FileCollectionItem {
    
    final InfoHash infoHash
    final String comment
    private final File file
    private final byte pieceSizePow2
    final long length
    private volatile byte[] payload
    final List<String> pathElements = new ArrayList<>()

    private final int hashCode
    
    public FileCollectionItem(InfoHash infoHash, String comment, List<String> pathElements, byte pieceSizePow2, long length) {
        this.infoHash = infoHash
        this.comment = comment
        this.pathElements = pathElements
        this.pieceSizePow2 = pieceSizePow2
        this.length = length
        
        File f = null
        pathElements.each { 
            if (f == null) {
                f = new File(it)
                return
            }
            f = new File(f, it)
        }
        this.file = f
        
        this.hashCode = Objects.hash(infoHash, comment, pathElements, pieceSizePow2, length)
    }
    
    public FileCollectionItem(InputStream is) {
        DataInputStream dis = new DataInputStream(is)
        byte version = dis.readByte()
        if (version != Constants.COLLECTION_ENTRY_VERSION)
            throw new InvalidCollectionException("unsupported entry version $version")
        
        byte [] hash = new byte[32]
        dis.readFully(hash)
        infoHash = new InfoHash(hash)
        
        pieceSizePow2 = dis.readByte()
        if (pieceSizePow2 < FileHasher.MIN_PIECE_SIZE_POW2 || pieceSizePow2 > FileHasher.MAX_PIECE_SIZE_POW2)
            throw new InvalidCollectionException("invalid piece size $pieceSizePow2")
        
        length = dis.readLong()
        if (length < 1 || length > FileHasher.MAX_SIZE)
            throw new InvalidCollectionException("invalid length $length")
        
        int nPathElements = dis.readUnsignedByte()
        File f = null
        nPathElements.times { 
            def element = new Name(dis)
            pathElements.add(element.name)
            if (f == null) {
                f = new File(element.name)
                return
            }
            f = new File(f, element.name)
        }
        file = f
        
        def commentName = new Name(dis)
        comment = commentName.name
        
        this.hashCode = Objects.hash(infoHash, comment, pathElements, pieceSizePow2, length)
    }
    
    void write(OutputStream os) {
        if (payload == null) {
            def baos = new ByteArrayOutputStream()
            def daos = new DataOutputStream(baos)
            
            daos.writeByte(Constants.COLLECTION_ENTRY_VERSION)
            daos.write(infoHash.getRoot())
            daos.writeByte(pieceSizePow2)
            daos.writeLong(length)
            daos.writeByte((byte) pathElements.size())
            pathElements.each { 
                def name = new Name(it)
                name.write(daos)
            }
            def commentName = new Name(comment)
            commentName.write(daos)
            daos.close()
            payload = baos.toByteArray()
        }
        os.write(payload)
    }
    
    @Override
    public int hashCode() {
        hashCode
    }
    
    @Override
    public boolean equals(Object o) {
        FileCollectionItem other = (FileCollectionItem) o
        
        return Objects.equals(infoHash, other.infoHash) &&
            Objects.equals(comment, other.comment) &&
            Objects.equals(pathElements, other.pathElements) &&
            pieceSizePow2 == other.pieceSizePow2 &&
            length == other.length
    }
}
