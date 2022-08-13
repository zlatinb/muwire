package com.muwire.core;

import net.i2p.data.Base64;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class SharedFile {

    private final File file;
    private final byte[] root;
    private final InfoHash rootInfoHash;
    private final int pieceSize;

    private final String cachedPath;
    private final long cachedLength;
    private final int hashCode;

    private volatile String comment;
    private List<String> downloaders = Collections.emptyList();
    private List<SearchEntry> searches = Collections.emptyList();
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

    private byte[] getPathHash() throws NoSuchAlgorithmException {
        MessageDigest digester = MessageDigest.getInstance("SHA-256");
        digester.update(file.getAbsolutePath().getBytes());
        return digester.digest();
    }

    public String getB64PathHash() throws NoSuchAlgorithmException {
        return Base64.encode(getPathHash());
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
    
    public synchronized int getHits() {
        return searches.size();
    }
    
    public synchronized void hit(Persona searcher, long timestamp, String query) {
        List<SearchEntry> empty = Collections.emptyList();
        if (searches == empty)
            searches = Collections.synchronizedList(new ArrayList<>());
        SearchEntry newEntry = new SearchEntry(searcher, timestamp, query);
        if (!searches.contains(newEntry))
            searches.add(newEntry);
    }
    
    public synchronized List<String> getDownloaders() {
        return downloaders;
    }
    
    public synchronized List<SearchEntry> getSearches() {
        return searches;
    }
    
    public synchronized void addDownloader(String name) {
        List<String> empty = Collections.emptyList();
        if (downloaders == empty)
            downloaders = Collections.synchronizedList(new ArrayList<>());
        if (!downloaders.contains(name))
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
