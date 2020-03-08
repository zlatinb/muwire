package com.muwire.core.filefeeds;

import com.muwire.core.Persona;

public class Feed {
    
    private final Persona publisher;
    
    private int updateInterval;
    private long lastUpdated;
    private int itemsToKeep;
    private boolean autoDownload;
    
    public Feed(Persona publisher) {
        this.publisher = publisher;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
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

}
