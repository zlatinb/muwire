package com.muwire.core.collections

import java.security.MessageDigest

import com.muwire.core.Constants
import com.muwire.core.InfoHash
import com.muwire.core.Name
import com.muwire.core.Persona
import com.muwire.core.util.DataUtil

import net.i2p.crypto.DSAEngine
import net.i2p.data.Signature
import net.i2p.data.SigningPrivateKey

class FileCollection {
    
    final long timestamp
    final Persona author
    final String comment
    private final byte[] sig
    private final int hashCode
    final String name
    
    private volatile byte[] payload
    private volatile InfoHash infoHash
    
    final Set<FileCollectionItem> files = new LinkedHashSet<>()
    
    final PathTree tree

    final List<SearchHit> hits = new ArrayList<>()
        
    FileCollection(long timestamp, Persona author, String comment, Set<FileCollectionItem> files,
        SigningPrivateKey spk) {
        this.timestamp = timestamp;
        this.author = author;
        this.comment = comment
        this.files = files
        
        name = files.first().pathElements.first()
        tree = new PathTree(name)
        for(FileCollectionItem item : files) {
            tree.add(item.pathElements, item)
        }
        
        byte [] signablePayload = signablePayload()
        Signature signature = DSAEngine.getInstance().sign(signablePayload, spk)
        this.sig = signature.getData()
        
        this.hashCode = Objects.hash(timestamp, author, comment, files)
    }
    
    FileCollection(InputStream is) {
        DataInputStream dis = new DataInputStream(is)
        byte version = (byte) dis.read()
        if (version != Constants.COLLECTION_VERSION)
            throw new InvalidCollectionException("unsupported version $version")
        
        int numFiles = dis.readUnsignedShort()
        
        author = new Persona(dis)
        timestamp = dis.readLong()
        def commentName = new Name(dis)
        comment = commentName.name
        
        numFiles.times { 
            files.add(new FileCollectionItem(dis))
        }
        
        sig = new byte[Constants.SIG_TYPE.getSigLen()]
        dis.readFully(sig)
        
        if (!verify())
            throw new InvalidCollectionException("invalid signature")
        
        name = files.first().pathElements.first()
        tree = new PathTree(name)
        for(FileCollectionItem item : files) {
            tree.add(item.pathElements, item)
        }
        
        this.hashCode = Objects.hash(timestamp, author, comment, files)
    }
    
    private boolean verify() {
        def baos = new ByteArrayOutputStream()
        def daos = new DataOutputStream(baos)
        
        daos.writeByte(Constants.COLLECTION_VERSION)
        daos.writeShort(files.size())
        author.write(daos)
        daos.writeLong(timestamp)
        def commentName = new Name(comment)
        commentName.write(daos)
        files.each { 
            it.write(daos)
        }
        
        daos.close()
        byte [] payload = baos.toByteArray()
        
        def spk = author.destination.getSigningPublicKey()
        def signature = new Signature(spk.getType(), sig)
        DSAEngine.getInstance().verifySignature(signature, payload, spk)
    }
    
    private byte[] signablePayload() {
        
        def baos = new ByteArrayOutputStream()
        def daos = new DataOutputStream(baos)
        
        daos.writeByte(Constants.COLLECTION_VERSION)
        daos.writeShort(files.size())
        author.write(daos)
        daos.writeLong(timestamp)
        def commentName = new Name(comment)
        commentName.write(daos)
        files.each { 
            it.write(daos)
        }
        
        daos.close()
        baos.toByteArray()
    }
    
    public void write(OutputStream os) {
        os.write(getPayload())
    }
    
    public byte[] getPayload() {
        if (payload == null) {
            def baos = new ByteArrayOutputStream()
            def daos = new DataOutputStream(baos)

            daos.write(signablePayload())
            daos.write(sig)

            daos.close()
            payload = baos.toByteArray()
        }
        payload
    }
    
    public InfoHash getInfoHash() {
        if (infoHash == null) {
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            digester.update(getPayload())
            infoHash = new InfoHash(digester.digest())
        }
        infoHash
    }
    
    public long totalSize() {
        long rv = 0
        files.each { 
            rv += it.length
        }
        rv
    }
    
    public int numFiles() {
        files.size()
    }
    
    public void hit(Persona searcher) {
        hits.add(new SearchHit(searcher))
    }
    
    @Override
    public int hashCode() {
        hashCode
    }
    
    @Override
    public boolean equals(Object o) {
        FileCollection other = (FileCollection)o
        timestamp == other.timestamp &&
            Objects.equals(author, other.author) &&
            Objects.equals(comment, other.comment) &&
            Objects.equals(files, other.files)
    }
    
    public static class SearchHit {
        final Persona searcher
        final long timestamp
        SearchHit(Persona searcher) {
            this.searcher = searcher
            this.timestamp = System.currentTimeMillis()
        }
    }
}
