package com.muwire.core.filefeeds;

public enum FeedFetchStatus {
    IDLE(false), 
    CONNECTING(true), 
    FETCHING(true), 
    FINISHED(false), 
    FAILED(false);
    
    private final boolean active;
    
    FeedFetchStatus(boolean active) {
        this.active = active;
    }
    
    public boolean isActive() {
        return active;
    }
}
