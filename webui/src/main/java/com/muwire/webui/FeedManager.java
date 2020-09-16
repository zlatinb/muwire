package com.muwire.webui;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.muwire.core.Core;
import com.muwire.core.Persona;
import com.muwire.core.SharedFile;
import com.muwire.core.filefeeds.Feed;
import com.muwire.core.filefeeds.FeedFetchEvent;
import com.muwire.core.filefeeds.FeedItem;
import com.muwire.core.filefeeds.FeedItemFetchedEvent;
import com.muwire.core.filefeeds.FeedLoadedEvent;
import com.muwire.core.filefeeds.UIDownloadFeedItemEvent;
import com.muwire.core.filefeeds.UIFeedConfigurationEvent;
import com.muwire.core.filefeeds.UIFeedDeletedEvent;
import com.muwire.core.filefeeds.UIFeedUpdateEvent;
import com.muwire.core.filefeeds.UIFilePublishedEvent;
import com.muwire.core.filefeeds.UIFileUnpublishedEvent;

public class FeedManager {
    
    private final Core core;
    private final FileManager fileManager;
    private final Map<Persona, RemoteFeed> remoteFeeds = new ConcurrentHashMap<>();
    private volatile long revision;
    
    public FeedManager(Core core, FileManager fileManager) {
        this.core = core;
        this.fileManager = fileManager;
    }
    
    public Map<Persona, RemoteFeed> getRemoteFeeds() {
        return remoteFeeds;
    }
    
    long getRevision() {
        return revision;
    }
    
    public void onFeedLoadedEvent(FeedLoadedEvent e) {
       remoteFeeds.put(e.getFeed().getPublisher(), new RemoteFeed(e.getFeed()));
       revision++;
    }
    
    public void onUIFeedConfigurationEvent(UIFeedConfigurationEvent e) {
        if (!e.isNewFeed()) 
            return;
        remoteFeeds.put(e.getFeed().getPublisher(), new RemoteFeed(e.getFeed()));
        revision++;
    }
    
    public void onFeedFetchEvent(FeedFetchEvent e) {
        RemoteFeed feed = remoteFeeds.get(e.getHost());
        if (feed == null)
            return; // hmm
        feed.getFeed().setStatus(e.getStatus());
        feed.revision++;
        revision++;
    }
    
    public void onFeedItemFetchedEvent(FeedItemFetchedEvent e) {
        FeedItem item = e.getItem();
        RemoteFeed feed = remoteFeeds.get(item.getPublisher());
        if (feed == null)
            return; // hmm
        
        if (feed.getFeed().isAutoDownload() && 
                !core.getFileManager().isShared(item.getInfoHash()) &&
                !core.getDownloadManager().isDownloading(item.getInfoHash())) {
            File target = new File(core.getMuOptions().getDownloadLocation(), item.getName());
            UIDownloadFeedItemEvent event = new UIDownloadFeedItemEvent();
            event.setItem(item);
            event.setTarget(target);
            event.setSequential(feed.getFeed().isSequential());
            core.getEventBus().publish(event);
        }
    }
    
    void subscribe(Persona publisher) {
        Feed feed = new Feed(publisher);
        feed.setAutoDownload(core.getMuOptions().getDefaultFeedAutoDownload());
        feed.setItemsToKeep(core.getMuOptions().getDefaultFeedItemsToKeep());
        feed.setUpdateInterval(core.getMuOptions().getDefaultFeedUpdateInterval());
        feed.setSequential(core.getMuOptions().getDefaultFeedSequential());
        UIFeedConfigurationEvent event = new UIFeedConfigurationEvent();
        event.setFeed(feed);
        event.setNewFeed(true);
        core.getEventBus().publish(event);
    }
    
    void unsubscribe(Persona publisher) {
        remoteFeeds.remove(publisher);
        UIFeedDeletedEvent event = new UIFeedDeletedEvent();
        event.setHost(publisher);
        core.getEventBus().publish(event);
    }
    
    void update(Persona publisher) {
        UIFeedUpdateEvent event = new UIFeedUpdateEvent();
        event.setHost(publisher);
        core.getEventBus().publish(event);
    }
    
    void configure(Persona publisher, boolean autoDownload, boolean sequential,
            long updateInterval, int itemsToKeep) {
        RemoteFeed rf = remoteFeeds.get(publisher);
        if (rf == null)
            return;
        Feed feed = rf.getFeed();
        feed.setAutoDownload(autoDownload);
        feed.setSequential(sequential);
        feed.setUpdateInterval(updateInterval);
        feed.setItemsToKeep(itemsToKeep);
        UIFeedConfigurationEvent event = new UIFeedConfigurationEvent();
        event.setFeed(feed);
        core.getEventBus().publish(event);
    }
    
    void publish(File file) {
        if (file.isFile()) {
            SharedFile sf = core.getFileManager().getFileToSharedFile().get(file);
            if (sf == null)
                return;
            sf.publish(System.currentTimeMillis());
            UIFilePublishedEvent event = new UIFilePublishedEvent();
            event.setSf(sf);
            core.getEventBus().publish(event);
        } else {
            long now = System.currentTimeMillis();
            for (SharedFile sf : fileManager.getAllFiles(file)) {
                if (!sf.isPublished())
                    sf.publish(now);
                UIFilePublishedEvent event = new UIFilePublishedEvent();
                event.setSf(sf);
                core.getEventBus().publish(event);
            }
        }
    }
    
    void unpublish(File file) {
        if (!file.isFile())
            throw new UnsupportedOperationException();
        SharedFile sf = core.getFileManager().getFileToSharedFile().get(file);
        if (sf == null)
            return;
        sf.unpublish();
        UIFileUnpublishedEvent event = new UIFileUnpublishedEvent();
        event.setSf(sf);
        core.getEventBus().publish(event);
    }
    
    static class RemoteFeed {
        private final Feed feed;
        private volatile long revision;
        
        RemoteFeed(Feed feed) {
            this.feed = feed;
        }
        
        public Feed getFeed() {
            return feed;
        }
        
        public long getRevision() {
            return revision;
        }
    }

}
