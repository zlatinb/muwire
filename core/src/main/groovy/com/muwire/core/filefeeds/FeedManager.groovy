package com.muwire.core.filefeeds

import com.muwire.core.UILoadedEvent

import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.stream.Collectors

import com.muwire.core.EventBus
import com.muwire.core.Persona

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log

import net.i2p.data.Base64
import net.i2p.util.ConcurrentHashSet

@Log
class FeedManager {
    
    private final EventBus eventBus
    private final File metadataFolder, itemsFolder
    private final Map<Persona, Feed> feeds = new ConcurrentHashMap<>()
    private final Map<Persona, Set<FeedItem>> feedItems = new ConcurrentHashMap<>()
    
    private final ExecutorService persister = Executors.newSingleThreadExecutor({r ->
        new Thread(r, "feed persister")
    } as ThreadFactory)
    
    
    FeedManager(EventBus eventBus, File home) {
        this.eventBus = eventBus
        File feedsFolder = new File(home, "filefeeds")
        if (!feedsFolder.exists())
            feedsFolder.mkdir()
        this.metadataFolder = new File(feedsFolder, "metadata")
        if (!metadataFolder.exists())
            metadataFolder.mkdir()
        this.itemsFolder = new File(feedsFolder, "items")
        if (!itemsFolder.exists())
            itemsFolder.mkdir()
    }
    
    public Feed getFeed(Persona persona) {
        feeds.get(persona)
    }
    
    public Set<FeedItem> getFeedItems(Persona persona) {
        feedItems.getOrDefault(persona, Collections.emptySet())
    }
    
    public List<Feed> getFeedsToUpdate() {
        long now = System.currentTimeMillis()
        feeds.values().stream().
            filter({Feed f -> !f.getStatus().isActive()}).
            filter({Feed f -> f.getLastUpdateAttempt() + f.getUpdateInterval() <= now})
            .collect(Collectors.toList())
    }
    
    void onUILoadedEvent(UILoadedEvent event) {
        log.info("starting feed manager")
        persister.submit({loadFeeds()} as Runnable)
        persister.submit({loadItems()} as Runnable)
    }
    
    void stop() {
        persister.shutdown()
    }
    
    private void loadFeeds() {
        def slurper = new JsonSlurper()
        Files.walk(metadataFolder.toPath()).
            filter( { it.getFileName().toString().endsWith(".json")}).
            forEach( {
                def parsed = slurper.parse(it.toFile())
                Persona publisher = new Persona(new ByteArrayInputStream(Base64.decode(parsed.publisher)))
                Feed feed = new Feed(publisher)
                feed.setUpdateInterval(parsed.updateInterval)
                feed.setLastUpdated(parsed.lastUpdated)
                feed.setLastUpdateAttempt(parsed.lastUpdateAttempt)
                feed.setItemsToKeep(parsed.itemsToKeep)
                feed.setAutoDownload(parsed.autoDownload)
                feed.setSequential(parsed.sequential)
                
                feed.setStatus(FeedFetchStatus.IDLE)
                
                feeds.put(feed.getPublisher(), feed)
                
                eventBus.publish(new FeedLoadedEvent(feed : feed))
            })     
    }
    
    private void loadItems() {
        def slurper = new JsonSlurper()
        feeds.keySet().each { persona ->
            File itemsFile = getItemsFile(feeds[persona])
            if (!itemsFile.exists())
                return // no items yet?
            itemsFile.eachLine { line ->
                def parsed = slurper.parseText(line)
                FeedItem item = FeedItems.objToFeedItem(parsed, persona)
                Set<FeedItem> items = feedItems.get(persona)
                if (items == null) {
                    items = new ConcurrentHashSet<>()
                    feedItems.put(persona, items)
                }
                items.add(item)
                eventBus.publish(new FeedItemLoadedEvent(item : item))
            }
        }
    }
    
    void onFeedItemFetchedEvent(FeedItemFetchedEvent e) {
        Set<FeedItem> set = feedItems.get(e.item.getPublisher())
        if (set == null) {
            set = new ConcurrentHashSet<>()
            feedItems.put(e.getItem().getPublisher(), set)
        }
        set.add(e.item)
    }
    
    void onFeedFetchEvent(FeedFetchEvent e) {
        
        Feed feed = feeds.get(e.host)
        if (feed == null) {
            log.severe("Fetching non-existent feed " + e.host.getHumanReadableName())
            return
        }
        
        feed.setStatus(e.status)
        
        if (e.status.isActive())
            return
        
        if (e.status == FeedFetchStatus.FINISHED) {
            feed.setStatus(FeedFetchStatus.IDLE)
            feed.setLastUpdated(e.getTimestamp())
        }
        // save feed items, then save feed.  This will save partial fetches too
        // which is ok because the items are stored in a Set
        persister.submit({saveFeedItems(e.host)} as Runnable)
        persister.submit({saveFeedMetadata(feed)} as Runnable)
    }
    
    void onUIFeedConfigurationEvent(UIFeedConfigurationEvent e) {
        feeds.put(e.feed.getPublisher(), e.feed)
        persister.submit({saveFeedMetadata(e.feed)} as Runnable)
    }
    
    void onUIFeedDeletedEvent(UIFeedDeletedEvent e) {
        Feed f = feeds.get(e.host)
        if (f == null) {
            log.severe("Deleting a non-existing feed " + e.host.getHumanReadableName())
            return
        }
        persister.submit({deleteFeed(f)} as Runnable)
    }
    
    private void saveFeedItems(Persona publisher) {
        Set<FeedItem> set = feedItems.get(publisher)
        if (set == null)
            return // can happen if nothing was published
        
        Feed feed = feeds[publisher]
        if (feed == null) {
            log.severe("Persisting items for non-existing feed " + publisher.getHumanReadableName())
            return
        }   
              
        if (feed.getItemsToKeep() == 0)
            return
             
        List<FeedItem> list = new ArrayList<>(set)
        if (feed.getItemsToKeep() > 0 && list.size() > feed.getItemsToKeep()) {
            log.info("will persist ${feed.getItemsToKeep()}/${list.size()} items")
            list.sort({l, r ->
                Long.compare(r.getTimestamp(), l.getTimestamp())
            } as Comparator<FeedItem>)
            list = list[0..feed.getItemsToKeep() - 1]
        }
        
        
        File itemsFile = getItemsFile(feed)
        itemsFile.withPrintWriter { writer -> 
            list.each { item -> 
                def obj = FeedItems.feedItemToObj(item)
                def json = JsonOutput.toJson(obj)
                writer.println(json)
            }
        }
    }
    
    private void saveFeedMetadata(Feed feed) {
        File metadataFile = getMetadataFile(feed)
        metadataFile.withPrintWriter { writer ->
            def json = [:]
            json.publisher = feed.getPublisher().toBase64()
            json.itemsToKeep = feed.getItemsToKeep()
            json.lastUpdated = feed.getLastUpdated()
            json.updateInterval = feed.getUpdateInterval()
            json.autoDownload = feed.isAutoDownload()
            json.sequential = feed.isSequential()
            json.lastUpdateAttempt = feed.getLastUpdateAttempt()
            json = JsonOutput.toJson(json)
            writer.println(json)
        }
    }
    
    private void deleteFeed(Feed feed) {
        feeds.remove(feed.getPublisher())
        feedItems.remove(feed.getPublisher())
        getItemsFile(feed).delete()
        getMetadataFile(feed).delete()
    }
    
    private File getItemsFile(Feed feed) {
        return new File(itemsFolder, feed.getPublisher().destination.toBase32() + ".json")
    }
    
    private File getMetadataFile(Feed feed) {
        return new File(metadataFolder, feed.getPublisher().destination.toBase32() + ".json")
    }
}
