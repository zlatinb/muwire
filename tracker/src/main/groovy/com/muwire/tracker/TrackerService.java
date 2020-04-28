package com.muwire.tracker;

public interface TrackerService {
    public TrackerStatus status();
    public void track(String infoHash);
}
