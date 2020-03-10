package com.muwire.core.filefeeds;

import java.util.Objects;

import com.muwire.core.InfoHash;
import com.muwire.core.Persona;

public class FeedItem {

    private final Persona publisher;
    private final long timestamp;
    private final String name;
    private final long size;
    private final int pieceSize;
    private final InfoHash infoHash;
    private final int certificates;
    private final String comment;
    
    public FeedItem(Persona publisher, long timestamp, String name, long size, int pieceSize, InfoHash infoHash,
            int certificates, String comment) {
        super();
        this.publisher = publisher;
        this.timestamp = timestamp;
        this.name = name;
        this.size = size;
        this.pieceSize = pieceSize;
        this.infoHash = infoHash;
        this.certificates = certificates;
        this.comment = comment;
    }

    public Persona getPublisher() {
        return publisher;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public InfoHash getInfoHash() {
        return infoHash;
    }

    public int getCertificates() {
        return certificates;
    }

    public String getComment() {
        return comment;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(publisher, timestamp, name, infoHash);
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FeedItem))
            return false;
        FeedItem other = (FeedItem)o;
        return Objects.equals(publisher, other.publisher) &&
                timestamp == other.timestamp &&
                Objects.equals(name, other.name) &&
                Objects.equals(infoHash, other.infoHash);
    }
}
