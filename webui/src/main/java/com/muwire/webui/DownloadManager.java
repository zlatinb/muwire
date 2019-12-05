package com.muwire.webui;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.download.DownloadStartedEvent;
import com.muwire.core.download.Downloader;
import com.muwire.core.download.UIDownloadCancelledEvent;

public class DownloadManager {
    private final Core core;
    
    private final Map<InfoHash,Downloader> downloaders = new ConcurrentHashMap<>();
    
    public DownloadManager(Core core) {
        this.core = core;
    }
    
    public void onDownloadStartedEvent(DownloadStartedEvent e) {
        downloaders.put(e.getDownloader().getInfoHash(),e.getDownloader());
    }
    
    public Stream<Downloader> getDownloaders() {
        return downloaders.values().stream();
    }
    
    public boolean isDownloading(InfoHash infoHash) {
        return downloaders.containsKey(infoHash);
    }
    
    void cancel(InfoHash infoHash) {
        Downloader d = downloaders.remove(infoHash);
        if (d == null)
            return;
        d.cancel();
        UIDownloadCancelledEvent event = new UIDownloadCancelledEvent();
        event.setDownloader(d);
        core.getEventBus().publish(event);
    }
}
