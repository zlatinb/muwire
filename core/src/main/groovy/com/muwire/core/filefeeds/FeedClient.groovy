package com.muwire.core.filefeeds

import java.lang.System.Logger.Level
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream

import com.muwire.core.EventBus
import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.util.DataUtil

import groovy.json.JsonSlurper
import groovy.util.logging.Log

@Log
class FeedClient {
    
    private final I2PConnector connector
    private final EventBus eventBus
    private final Persona me
    private final FeedManager feedManager
    
    private final ExecutorService feedFetcher = Executors.newCachedThreadPool() 
    private final Timer feedUpdater = new Timer("feed-updater", true)
    
    FeedClient(I2PConnector connector, EventBus eventBus, Persona me, FeedManager feedManager) {
        this.connector = connector
        this.eventBus = eventBus
        this.me = me
        this.feedManager = feedManager
    }
    
    private void start() {
        feedUpdater.schedule({updateAnyFeeds()} as TimerTask, 60000, 60000)
    }
    
    private void stop() {
        feedUpdater.cancel()
        feedFetcher.shutdown()
    }
    
    private void updateAnyFeeds() {
        feedManager.getFeedsToUpdate().each { 
            feedFetcher.execute({updateFeed(it)} as Runnable)
        }
    }
    
    void onUIFeedUpdateEvent(UIFeedUpdateEvent e) {
        Feed feed = feedManager.getFeed(e.host)
        if (feed == null) {
            log.severe("UI request to update non-existent feed " + e.host.getHumanReadableName())
            return
        }
        
        feedFetcher.execute({updateFeed(feed)} as Runnable)
    }
    
    private void updateFeed(Feed feed) {
        log.info("updating feed " + feed.getPublisher().getHumanReadableName())
        Endpoint endpoint = null
        try {
            eventBus.publish(new FeedFetchEvent(host : feed.getPublisher(), status : FeedFetchStatus.CONNECTING))
            endpoint = connector.connect(feed.getPublisher().getDestination())
            OutputStream os = endpoint.getOutputStream()
            os.write("FEED\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Persona:${me.toBase64()}\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Timestamp:${feed.getLastUpdated()}\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
            os.flush()
            
            InputStream is = endpoint.getInputStream()
            String code = DataUtil.readTillRN(is)
            if (!code.startsWith("200"))
                throw new IOException("Invalid code $code")
                
            // parse all headers
            Map<String,String> headers = DataUtil.readAllHeaders(is)
            
            if (!headers.containsKey("Count"))
                throw new IOException("No count header")
            
            int items = Integer.parseInt(headers['Count'])
            
            eventBus.publish(new FeedFetchEvent(host : feed.getPublisher(), status : FeedFetchStatus.FETCHING, totalItems: items))
            
            JsonSlurper slurper = new JsonSlurper()
            DataInputStream dis = new DataInputStream(new GZIPInputStream(is))
            for (int i = 0; i < items; i++) {
                int size = dis.readUnsignedShort()
                byte [] tmp = new byte[size]
                dis.readFully(tmp)
                def json = slurper.parse(tmp)
                FeedItem item = FeedItems.objToFeedItem(json, feed.getPublisher())
                eventBus.publish(new FeedItemFetchedEvent(item: item))
            }
            
            eventBus.publish(new FeedFetchEvent(host : feed.getPublisher(), status : FeedFetchStatus.FINISHED))
        } catch (Exception bad) {
            log.log(Level.WARNING, "Feed update failed", bad)
            eventBus.publish(new FeedFetchEvent(host : feed.getPublisher(), status : FeedFetchStatus.FAILED)) 
        } finally {
            endpoint?.close()
        }
    }
}
