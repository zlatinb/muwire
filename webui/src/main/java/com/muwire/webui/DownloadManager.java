package com.muwire.webui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.download.DownloadStartedEvent;
import com.muwire.core.download.Downloader;

public class DownloadManager {
    private final Core core;
    
    private final List<Downloader> downloaders = new CopyOnWriteArrayList<>();
    
    public DownloadManager(Core core) {
        this.core = core;
    }
    
    public void onDownloadStartedEvent(DownloadStartedEvent e) {
        downloaders.add(e.getDownloader());
    }
    
    public List<Downloader> getDownloaders() {
        return downloaders;
    }
    
    public boolean isDownloading(InfoHash infoHash) {
        return !downloaders.stream().map(d -> d.getInfoHash()).filter(d -> d.equals(infoHash)).findAny().isEmpty();
    }
}
