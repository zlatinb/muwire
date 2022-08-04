package com.muwire.gui.mulinks

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.collections.FileCollection
import net.i2p.crypto.DSAEngine
import net.i2p.data.SigningPrivateKey

import java.nio.charset.StandardCharsets

class CollectionMuLink extends MuLink {
    final int numFiles
    final long totalSize
    
    CollectionMuLink(Persona host, InfoHash infoHash, String name, byte[] sig, 
        Map<String,String> query) 
        throws InvalidMuLinkException {
        super(host, infoHash, name, sig, LinkType.COLLECTION)
        
        int numFiles
        long totalSize
        
        try {
            numFiles = Integer.parseInt(query.numFiles)
            totalSize = Long.parseLong(query.totalSize)
        } catch (NumberFormatException e) {
            throw new InvalidMuLinkException(e)
        }
        
        this.numFiles = numFiles
        this.totalSize = totalSize
        
        if (numFiles <= 0 || totalSize <= 0)
            throw new InvalidMuLinkException("numFiles $numFiles totalSize $totalSize")
    }
    
    CollectionMuLink(FileCollection collection, Persona me, SigningPrivateKey spk) {
        super(me, collection.getInfoHash(), collection.getName(),
            deriveSig(collection, spk),
            LinkType.COLLECTION)
        numFiles = collection.numFiles()
        totalSize = collection.totalSize()
    }
    
    private static byte[] deriveSig(FileCollection collection, SigningPrivateKey spk) {
        def baos = new ByteArrayOutputStream()
        def daos = new DataOutputStream(baos)
        
        daos.write(collection.getInfoHash().getRoot())
        daos.write(collection.getName().getBytes(StandardCharsets.UTF_8))
        daos.writeByte(LinkType.COLLECTION.ordinal())
        daos.writeInt(collection.numFiles())
        daos.writeLong(collection.totalSize())
        
        daos.flush()
        byte [] payload = baos.toByteArray()
        DSAEngine.getInstance().sign(payload, spk).getData()
    }

    @Override
    protected void appendSignedElements(ByteArrayOutputStream baos) {
        DataOutputStream daos = new DataOutputStream(baos)
        daos.writeInt(numFiles)
        daos.writeLong(totalSize)
        daos.flush()
    }

    @Override
    protected void addQueryElements(Map<String, String> query) {
        query.numFiles = String.valueOf(numFiles)
        query.totalSize = String.valueOf(totalSize)
    }
}
