package com.muwire.core.messenger

import com.muwire.core.Constants
import com.muwire.core.InfoHash
import com.muwire.core.Name
import com.muwire.core.files.FileHasher

class MWMessageAttachment {
    final InfoHash infoHash
    final String name
    final long length
    final byte pieceSizePow2
    
    private volatile byte[] payload
    private final int hashCode
    
    public MWMessageAttachment(InfoHash infoHash, String name, long length, byte pieceSizePow2) {
        this.infoHash = infoHash
        this.name = name
        this.length = length
        this.pieceSizePow2 = pieceSizePow2
        
        hashCode = Objects.hash(infoHash, name, length, pieceSizePow2)
    }
    
    public MWMessageAttachment(InputStream is) {
        DataInputStream dis = new DataInputStream(is)
        byte version = dis.readByte()
        if (version != Constants.MESSENGER_ATTACHMENT_VERSION)
            throw new InvalidMessageException("unknown version $version")
        
        byte [] ih = new byte[InfoHash.SIZE]
        dis.readFully(ih)
        infoHash = new InfoHash(ih)
        
        pieceSizePow2 = dis.readByte()
        if (pieceSizePow2 < FileHasher.MIN_PIECE_SIZE_POW2 || pieceSizePow2 > FileHasher.MAX_PIECE_SIZE_POW2)
            throw new InvalidMessageException("invalid piece size $pieceSizePow2")
        
        length = dis.readLong()
        if (length < 1 || length > FileHasher.MAX_SIZE)
            throw new InvalidMessageException("invalid length $length")
            
         def n = new Name(dis)
         name = n.name
         
         hashCode = Objects.hash(infoHash, name, length, pieceSizePow2)
    }
    
    void write(OutputStream os) {
        if (payload == null) {
            def baos = new ByteArrayOutputStream()
            def daos = new DataOutputStream(baos)
            
            daos.writeByte(Constants.MESSENGER_ATTACHMENT_VERSION)
            daos.write(infoHash.getRoot())
            daos.writeByte(pieceSizePow2)
            daos.writeLong(length)
            def n = new Name(name)
            n.write(daos)
            daos.close()
            payload = baos.toByteArray()
        }
        os.write(payload)
    }
    
    public int hashCode() {
        hashCode
    }
    
    public boolean equals(Object o) {
        MWMessageAttachment other = (MWMessageAttachment) o
        return Objects.equals(infoHash, other.infoHash) &&
            Objects.equals(name, other.name) &&
            length == other.length &&
            pieceSizePow2 == other.pieceSizePow2
    }
}
