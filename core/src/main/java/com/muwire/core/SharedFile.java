package com.muwire.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.muwire.core.util.DataUtil;

import net.i2p.data.Base64;

public class SharedFile {

    private final File file;
    private final InfoHash infoHash;
    private final int pieceSize;

    private final String cachedPath;
    private final long cachedLength;
    
    private final String b64EncodedFileName;
    private final String b64EncodedHashRoot;
    private final List<String> b64EncodedHashList;
    
    private volatile String comment;

    public SharedFile(File file, InfoHash infoHash, int pieceSize) throws IOException {
        this.file = file;
        this.infoHash = infoHash;
        this.pieceSize = pieceSize;
        this.cachedPath = file.getAbsolutePath();
        this.cachedLength = file.length();
        this.b64EncodedFileName = Base64.encode(DataUtil.encodei18nString(file.toString()));
        this.b64EncodedHashRoot = Base64.encode(infoHash.getRoot());
        
        List<String> b64List = new ArrayList<String>();
        byte[] tmp = new byte[32];
        for (int i = 0; i < infoHash.getHashList().length / 32; i++) {
            System.arraycopy(infoHash.getHashList(), i * 32, tmp, 0, 32);
            b64List.add(Base64.encode(tmp));
        }
        this.b64EncodedHashList = b64List;
    }

    public File getFile() {
        return file;
    }

    public InfoHash getInfoHash() {
        return infoHash;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public int getNPieces() {
        long length = file.length();
        int rawPieceSize = 0x1 << pieceSize;
        int rv = (int) (length / rawPieceSize);
        if (length % rawPieceSize != 0)
            rv++;
        return rv;
    }
    
    public String getB64EncodedFileName() {
        return b64EncodedFileName;
    }
    
    public String getB64EncodedHashRoot() {
        return b64EncodedHashRoot;
    }
    
    public List<String> getB64EncodedHashList() {
        return b64EncodedHashList;
    }

    public String getCachedPath() {
        return cachedPath;
    }

    public long getCachedLength() {
        return cachedLength;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public String getComment() {
        return comment;
    }

    @Override
    public int hashCode() {
        return file.hashCode() ^ infoHash.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SharedFile))
            return false;
        SharedFile other = (SharedFile)o;
        return file.equals(other.file) && infoHash.equals(other.infoHash);
    }
}
