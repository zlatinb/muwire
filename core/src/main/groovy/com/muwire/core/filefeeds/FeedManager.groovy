package com.muwire.core.filefeeds

import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

import com.muwire.core.EventBus
import com.muwire.core.Persona

import groovy.json.JsonSlurper
import groovy.util.logging.Log

import net.i2p.data.Base64

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
    
    void start() {
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
                feed.setItemsToKeep(parsed.itemsToKeep)
                feed.setAutoDownload(parsed.autoDownload)
                
                feeds.put(feed.getPublisher(), feed)
                
                eventBus.publish(new FeedLoadedEvent(feed : feed))
            })     
    }
    
    private void loadItems() {
        def slurper = new JsonSlurper()
        feeds.keySet().each {
            File itemsFile = new File(itemsFolder, it.destination.toBase32() + ".json")
            if (!itemsFile.exists())
                return // no items yet?
            itemsFile.eachLine { line ->
                def parsed = slurper.parse(line)
                FeedItem item = FeedItems.objToFeedItem(parsed, it)
                
                Set<FeedItem> items = feedItems.get(it)
                if (items == null) {
                    items = new HashSet<>()
                    feedItems.put(it, items)
                }
                items.add(item)
                
                eventBus.publish(new FeedItemLoadedEvent(item : item))
            }
        }
    }
}
