package com.muwire.webui;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.download.DownloadStartedEvent;
import com.muwire.core.download.Downloader;
import com.muwire.core.download.UIDownloadCancelledEvent;
import com.muwire.core.download.UIDownloadPausedEvent;
import com.muwire.core.download.UIDownloadResumedEvent;

public class DownloadManager {
    private final Core core;
    
    private final Map<InfoHash,Downloader> downloaders = new ConcurrentHashMap<>();
    
    private final Timer retryTimer = new Timer("download-resumer", true);
    private long lastRetryTime;
    
    public DownloadManager(Core core) {
        this.core = core;
        retryTimer.schedule(new ResumeTask(), 1000, 1000);
    }
    
    void shutdown() {
        retryTimer.cancel();
    }
    
    public void onDownloadStartedEvent(DownloadStartedEvent e) {
        downloaders.put(e.getDownloader().getInfoHash(),e.getDownloader());
    }
    
    public Stream<Downloader> getDownloaders() {
        return downloaders.values().stream();
    }
    
    public boolean isDownloading(InfoHash infoHash) {
        return downloaders.containsKey(infoHash) &&
                downloaders.get(infoHash).getCurrentState() != Downloader.DownloadState.CANCELLED;
    }
    
    void cancel(InfoHash infoHash) {
        Downloader d = downloaders.get(infoHash);
        if (d == null)
            return;
        d.cancel();
        UIDownloadCancelledEvent event = new UIDownloadCancelledEvent();
        event.setDownloader(d);
        core.getEventBus().publish(event);
        delay();
    }
    
    void clearFinished() {
        for(Iterator<Map.Entry<InfoHash, Downloader>> iter = downloaders.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<InfoHash, Downloader> entry = iter.next();
            Downloader.DownloadState state = entry.getValue().getCurrentState();
            if (state == Downloader.DownloadState.CANCELLED ||
                    state == Downloader.DownloadState.FINISHED ||
                    state == Downloader.DownloadState.HOPELESS)
                iter.remove();
        }
    }
    
    private static void delay() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
    }
    
    void pause(InfoHash infoHash) {
        Downloader d = downloaders.get(infoHash);
        if (d == null)
            return;
        d.pause();
        UIDownloadPausedEvent event = new UIDownloadPausedEvent();
        core.getEventBus().publish(event);
        delay();
    }
    
    void resume(InfoHash infoHash) {
        Downloader d = downloaders.get(infoHash);
        if (d == null)
            return;
        d.resume();
        UIDownloadResumedEvent event = new UIDownloadResumedEvent();
        core.getEventBus().publish(event);
        delay();
    }
    
    private class ResumeTask extends TimerTask {

        @Override
        public void run() {
            if (core.getShutdown().get())
                return;
            int retryInterval = core.getMuOptions().getDownloadRetryInterval();
            if (retryInterval > 0) {
                retryInterval *= 1000;
                long now = System.currentTimeMillis();
                if (now - lastRetryTime > retryInterval) {
                    lastRetryTime = now;
                    for (Downloader d : downloaders.values()) {
                        Downloader.DownloadState state = d.getCurrentState();
                        if (state == Downloader.DownloadState.FAILED ||
                                state == Downloader.DownloadState.DOWNLOADING) 
                            d.resume();
                    }
                }
            }
        }
        
    }
}
