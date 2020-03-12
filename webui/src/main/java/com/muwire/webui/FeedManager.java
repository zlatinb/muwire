package com.muwire.webui;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.muwire.core.Core;
import com.muwire.core.Persona;
import com.muwire.core.filefeeds.Feed;
import com.muwire.core.filefeeds.FeedFetchEvent;
import com.muwire.core.filefeeds.FeedItem;
import com.muwire.core.filefeeds.FeedItemFetchedEvent;
import com.muwire.core.filefeeds.FeedLoadedEvent;
import com.muwire.core.filefeeds.UIDownloadFeedItemEvent;
import com.muwire.core.filefeeds.UIFeedConfigurationEvent;
import com.muwire.core.filefeeds.UIFeedDeletedEvent;

public class FeedManager {
    
    private final Core core;
    private final Map<Persona, RemoteFeed> remoteFeeds = new ConcurrentHashMap<>();
    
    public FeedManager(Core core) {
        this.core = core;
    }
    
    public Map<Persona, RemoteFeed> getRemoteFeeds() {
        return remoteFeeds;
    }
    
    public void onFeedLoadedEvent(FeedLoadedEvent e) {
       remoteFeeds.put(e.getFeed().getPublisher(), new RemoteFeed(e.getFeed()));
    }
    
    public void onUIFeedConfigurationEvent(UIFeedConfigurationEvent e) {
        if (!e.isNewFeed()) 
            return;
        remoteFeeds.put(e.getFeed().getPublisher(), new RemoteFeed(e.getFeed()));
    }
    
    public void onFeedFetchEvent(FeedFetchEvent e) {
        RemoteFeed feed = remoteFeeds.get(e.getHost());
        if (feed == null)
            return; // hmm
        feed.getFeed().setStatus(e.getStatus());
        feed.revision++;
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
