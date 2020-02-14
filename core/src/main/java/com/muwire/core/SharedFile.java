package com.muwire.core;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.muwire.core.util.DataUtil;

import net.i2p.data.Base64;

public class SharedFile {

    private final File file;
    private final byte[] root;
    private final int pieceSize;

    private final String cachedPath;
    private final long cachedLength;

    private String b64PathHash;
    private final String b64EncodedFileName;
    
    private volatile String comment;
    private final Set<String> downloaders = Collections.synchronizedSet(new HashSet<>());
    private final Set<SearchEntry> searches = Collections.synchronizedSet(new HashSet<>());

    public SharedFile(File file, byte[] root, int pieceSize) throws IOException {
        this.file = file;
        this.root = root;
        this.pieceSize = pieceSize;
        this.cachedPath = file.getAbsolutePath();
        this.cachedLength = file.length();
        this.b64EncodedFileName = Base64.encode(DataUtil.encodei18nString(file.toString()));
    }

    public File getFile() {
        return file;
    }

    public byte[] getPathHash() throws NoSuchAlgorithmException {
        MessageDigest digester = MessageDigest.getInstance("SHA-256");
        digester.update(file.getAbsolutePath().getBytes());
        return digester.digest();
    }

    public String getB64PathHash() throws NoSuchAlgorithmException {
        if(b64PathHash == null){
            b64PathHash = Base64.encode(getPathHash());
        }
        return b64PathHash;
    }

    public byte[] getRoot() {
        return root;
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
    
    public int getHits() {
        return searches.size();
    }
    
    public void hit(Persona searcher, long timestamp, String query) {
        searches.add(new SearchEntry(searcher, timestamp, query));
    }
    
    public Set<String> getDownloaders() {
        return downloaders;
    }
    
    public Set<SearchEntry> getSearches() {
        return searches;
    }
    
    public void addDownloader(String name) {
        downloaders.add(name);
    }

    @Override
    public int hashCode() {
        return file.hashCode() ^ Arrays.hashCode(root);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SharedFile))
            return false;
        SharedFile other = (SharedFile)o;
        return file.equals(other.file) && Arrays.equals(root, other.root);
    }
    
    public static class SearchEntry {
        private final Persona searcher;
        private final long timestamp;
        private final String query;
        
        public SearchEntry(Persona searcher, long timestamp, String query) {
            this.searcher = searcher;
            this.timestamp = timestamp;
            this.query = query;
        }
        
        public int hashCode() {
            return Objects.hash(searcher) ^ Objects.hash(timestamp) ^ query.hashCode();
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof SearchEntry))
                return false;
            SearchEntry other = (SearchEntry)o;
            return Objects.equals(searcher, other.searcher) &&
                    timestamp == other.timestamp &&
                    query.equals(other.query);
        }
    }
}
