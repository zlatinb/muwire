package com.muwire.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
    private final InfoHash rootInfoHash;
    private final int pieceSize;

    private final String cachedPath;
    private final long cachedLength;
    private final int hashCode;

    private String b64PathHash;
    
    private volatile String comment;
    private Set<String> downloaders = Collections.emptySet();
    private Set<SearchEntry> searches = Collections.emptySet();
    private volatile boolean published;
    private volatile long publishedTimestamp;
    
    /** Path to the top-most parent File that is shared.  Null if no such exists */
    private volatile Path pathToSharedParent;
    private volatile String cachedVisiblePath;

    public SharedFile(File file, byte[] root, int pieceSize) throws IOException {
        this.file = file;
        this.root = root;
        this.rootInfoHash = new InfoHash(root);
        this.pieceSize = pieceSize;
        this.cachedPath = file.getAbsolutePath();
        this.cachedLength = file.length();
        this.hashCode = Arrays.hashCode(root) ^ file.hashCode();
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

    public InfoHash getRootInfoHash() {return rootInfoHash;}

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
        Set<SearchEntry> empty = Collections.emptySet();
        if (searches == empty)
            searches = Collections.synchronizedSet(new HashSet<>());
        searches.add(new SearchEntry(searcher, timestamp, query));
    }
    
    public Set<String> getDownloaders() {
        return downloaders;
    }
    
    public Set<SearchEntry> getSearches() {
        return searches;
    }
    
    public void addDownloader(String name) {
        Set<String> empty = Collections.emptySet();
        if (downloaders == empty)
            downloaders = Collections.synchronizedSet(new HashSet<>());
        downloaders.add(name);
    }
    
    public void publish(long timestamp) {
        published = true;
        publishedTimestamp = timestamp;
    }
    
    public void unpublish() {
        published = false;
        publishedTimestamp = 0;
    }
    
    public boolean isPublished() {
        return published;
    }
    
    public long getPublishedTimestamp() {
        return publishedTimestamp;
    }

    /**
     * Sets the path to the shared parent and computes
     * the cached visible path.
     */
    public void setPathToSharedParent(Path path) {
        this.pathToSharedParent = path;
        
        String shortPath;
        if (pathToSharedParent.getNameCount() > 1) {
            Path tmp = pathToSharedParent.subpath(1, pathToSharedParent.getNameCount());
            shortPath = "..." + File.separator + tmp;
        } else {
            shortPath = "...";
        }
        shortPath += File.separator + getFile().getName();
        this.cachedVisiblePath = shortPath;
    }
    
    public Path getPathToSharedParent() {
        return pathToSharedParent;
    }
    
    public String getCachedVisiblePath() {
        return cachedVisiblePath;
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SharedFile))
            return false;
        SharedFile other = (SharedFile)o;
        return Arrays.equals(root, other.root) && file.equals(other.file);
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
        
        public Persona getSearcher() {
            return searcher;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public String getQuery() {
            return query;
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
