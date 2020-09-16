package com.muwire.core.filefeeds;

import com.muwire.core.Persona;

public class Feed {
    
    private final Persona publisher;
    
    private long updateInterval;
    private long lastUpdated;
    private volatile long lastUpdateAttempt;
    private int itemsToKeep;
    private boolean autoDownload;
    private boolean sequential;
    private FeedFetchStatus status;
    
    public Feed(Persona publisher) {
        this.publisher = publisher;
        this.status = FeedFetchStatus.IDLE;
    }

    public long getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(long updateInterval) {
        this.updateInterval = updateInterval;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public int getItemsToKeep() {
        return itemsToKeep;
    }

    public void setItemsToKeep(int itemsToKeep) {
        this.itemsToKeep = itemsToKeep;
    }

    public boolean isAutoDownload() {
        return autoDownload;
    }

    public void setAutoDownload(boolean autoDownload) {
        this.autoDownload = autoDownload;
    }

    public Persona getPublisher() {
        return publisher;
    }

    public void setStatus(FeedFetchStatus status) {
        this.status = status;
    }
    
    public FeedFetchStatus getStatus() {
        return status;
    }
    
    public void setSequential(boolean sequential) {
        this.sequential = sequential;
    }
    
    public boolean isSequential() {
        return sequential;
    }
    
    public void setLastUpdateAttempt(long lastUpdateAttempt) {
        this.lastUpdateAttempt = lastUpdateAttempt;
    }
    
    public long getLastUpdateAttempt() {
        return lastUpdateAttempt;
    }
}
