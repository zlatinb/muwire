package com.muwire.core.collections

import com.muwire.core.Constants
import com.muwire.core.InfoHash
import com.muwire.core.Name

class FileCollectionItem {
    
    private final InfoHash infoHash
    private final String comment
    private final File file
    private volatile byte[] payload
    private final List<String> pathElements = new ArrayList<>()

    public FileCollectionItem(InfoHash infoHash, String comment, List<String> pathElements) {
        this.infoHash = infoHash
        this.comment = comment
        this.pathElements = pathElements
        
        File f = null
        pathElements.each { 
            if (f == null) {
                f = new File(it)
                return
            }
            f = new File(f, it)
        }
        this.file = f
    }
    
    public FileCollectionItem(InputStream is) {
        DataInputStream dis = new DataInputStream(is)
        byte version = dis.readByte()
        if (version != Constants.COLLECTION_ENTRY_VERSION)
            throw new InvalidCollectionException("unsupported entry version $version")
        
        byte [] hash = new byte[32]
        dis.readFully(hash)
        infoHash = new InfoHash(hash)
        
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
    }
    
    void write(OutputStream os) {
        if (payload == null) {
            def baos = new ByteArrayOutputStream()
            def daos = new DataOutputStream(baos)
            
            daos.writeByte(Constants.COLLECTION_ENTRY_VERSION)
            daos.write(infoHash.getRoot())
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
}
