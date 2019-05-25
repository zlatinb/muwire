package com.muwire.core.connection

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

import com.muwire.core.EventBus
import com.muwire.core.hostcache.HostCache
import com.muwire.core.hostcache.HostDiscoveredEvent
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
abstract class Connection implements Closeable {

	final EventBus eventBus
	final Endpoint endpoint
	final boolean incoming
	final HostCache hostCache
    final TrustService trustService
	
	private final AtomicBoolean running = new AtomicBoolean()
	private final BlockingQueue messages = new LinkedBlockingQueue()
	private final Thread reader, writer
	
	protected final String name
	
	long lastPingSentTime, lastPongReceivedTime
	
	Connection(EventBus eventBus, Endpoint endpoint, boolean incoming, HostCache hostCache, TrustService trustService) {
		this.eventBus = eventBus
		this.incoming = incoming
		this.endpoint = endpoint
		this.hostCache = hostCache
        this.trustService = trustService
		
		this.name = endpoint.destination.toBase32().substring(0,8)
		
		this.reader = new Thread({readLoop()} as Runnable)
		this.reader.setName("reader-$name")
		this.reader.setDaemon(true)
		
		this.writer = new Thread({writeLoop()} as Runnable)
		this.writer.setName("writer-$name")
		this.writer.setDaemon(true)
	}
	
	/**
	 * starts the connection threads
	 */
	void start() {
		if (!running.compareAndSet(false, true)) {
			log.log(Level.WARNING,"$name already running", new Exception())
			return
		}
		reader.start()
		writer.start()
	}
	
	@Override
	public void close() {
		if (!running.compareAndSet(true, false)) {
			log.log(Level.WARNING, "$name already closed", new Exception() )
			return
		}
		reader.interrupt()
		writer.interrupt()
		eventBus.publish(new DisconnectionEvent(destination: endpoint.destination))
	}
	
	protected void readLoop() {
		try {
			while(running.get()) {
				read()
			}
		} catch (SocketTimeoutException e) {
			close()
		} catch (Exception e) {
			if (running.get()) {
				log.log(Level.WARNING,"unhandled exception in reader",e)
				close()
			}
		}
	}
	
	protected abstract void read()
	
	protected void writeLoop() {
		try {
			while(running.get()) {
				def message = messages.take()
				write(message)
			}
		} catch (Exception e) {
			if (running.get()) {
				log.log(Level.WARNING, "unhandled exception in writer",e)
				close()
			}
		}
	}
	
	protected abstract void write(def message);
	
	void sendPing() {
		def ping = [:]
		ping.type = "Ping"
		ping.version = 1
		messages.put(ping)
		lastPingSentTime = System.currentTimeMillis()
	}
    
    void sendQuery(QueryEvent e) {
        def query = [:]
        query.type = "Search"
        query.version = 1
        query.uuid = e.searchEvent.getUuid()
        // TODO: first hop figure out
        query.keywords = e.searchEvent.getSearchTerms()
        query.replyTo = e.getReceivedOn().toBase64()
        messages.put(query)
    }
	
	protected void handlePing() {
		log.fine("$name received ping")
		def pong = [:]
		pong.type = "Pong"
		pong.version = 1
		pong.pongs = hostCache.getGoodHosts(10).collect { d -> d.toBase64() }
		messages.put(pong)
	}
	
	protected void handlePong(def pong) {
		log.fine("$name received pong")
		lastPongReceivedTime = System.currentTimeMillis()
		if (pong.pongs == null)
			throw new Exception("Pong doesn't have pongs")
		pong.pongs.each { 
			def dest = new Destination(it)
			eventBus.publish(new HostDiscoveredEvent(destination: dest))
		}
	}
    
    protected void handleSearch(def search) {
        UUID uuid = UUID.fromString(search.uuid)
        if (search.infohash != null)
            search.keywords = null
            
        Destination replyTo = new Destination(search.replyTo)
        if (trustService.getLevel(replyTo) == TrustLevel.DISTRUSTED) {
            log.info "dropping search from distrusted peer"
            return
        }
        // TODO: add option to respond only to trusted peers
        
        SearchEvent searchEvent = new SearchEvent(searchTerms : search.keywords,
                                            searchHash : search.infohash,
                                            uuid : uuid)
        QueryEvent event = new QueryEvent ( searchEvent : searchEvent,
                                            replyTo : replyTo,
                                            receivedOn : endpoint.destination,
                                            firstHop : search.firstHop )
        eventBus.publish(event)
                                            
    }
}
