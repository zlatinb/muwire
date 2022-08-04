package com.muwire.gui.mulinks

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.SharedFile
import com.muwire.core.files.FileHasher
import net.i2p.crypto.DSAEngine
import net.i2p.data.SigningPrivateKey

import java.nio.charset.StandardCharsets
import java.nio.file.Path

class FileMuLink extends MuLink {
    final long fileSize
    final int pieceSizePow2
    
    FileMuLink(Persona host, InfoHash infoHash, String name, byte[] sig, Map<String, String> query)
    throws InvalidMuLinkException {
        super(host, infoHash, name, sig, LinkType.FILE)
        
        long fileSize
        int pieceSizePow2
        try {
            fileSize = Long.parseLong(query.fileSize)
            pieceSizePow2 = Integer.parseInt(query.pieceSizePow2)
        } catch (NumberFormatException e) {
            throw new InvalidMuLinkException(e)
        }
        
        this.fileSize = fileSize
        this.pieceSizePow2 = pieceSizePow2
        
        if (fileSize <= 0 || fileSize > FileHasher.MAX_SIZE)
            throw new InvalidMuLinkException("invalid size $fileSize")
        if (pieceSizePow2 < FileHasher.MIN_PIECE_SIZE_POW2 || pieceSizePow2 > FileHasher.MAX_PIECE_SIZE_POW2)
            throw new InvalidMuLinkException("invalid piece size $pieceSizePow2")
    }

    FileMuLink(SharedFile sharedFile, Persona me, SigningPrivateKey spk) {
        super(me, sharedFile.getRootInfoHash(), sharedFile.getFile().getName(),
            deriveSig(sharedFile, spk),
            LinkType.FILE)
        fileSize = sharedFile.getCachedLength()
        pieceSizePow2 = sharedFile.getPieceSize()
    }
    
    private static byte[] deriveSig(SharedFile sharedFile, SigningPrivateKey spk) {
        def baos = new ByteArrayOutputStream()
        def daos = new DataOutputStream(baos)
        
        daos.write(sharedFile.getRoot())
        daos.write(sharedFile.getFile().getName().getBytes(StandardCharsets.UTF_8))
        daos.writeByte(LinkType.FILE.ordinal())
        daos.writeLong(sharedFile.getCachedLength())
        daos.writeByte(sharedFile.getPieceSize())
        
        daos.flush()
        byte [] payload = baos.toByteArray()
        DSAEngine.getInstance().sign(payload, spk).getData()
    }
    
    @Override
    protected void appendSignedElements(ByteArrayOutputStream baos) {
        DataOutputStream daos = new DataOutputStream(baos)
        daos.writeLong(fileSize)
        daos.writeByte(pieceSizePow2)
        daos.flush()
    }
    
    @Override
    protected void addQueryElements(Map<String,String> query) {
        query.fileSize = String.valueOf(fileSize)
        query.pieceSizePow2 = String.valueOf(pieceSizePow2)
    }
}
