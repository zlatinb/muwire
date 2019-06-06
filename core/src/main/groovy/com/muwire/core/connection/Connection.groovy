package com.muwire.core.connection

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.hostcache.HostCache
import com.muwire.core.hostcache.HostDiscoveredEvent
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService

import groovy.util.logging.Log
import net.i2p.data.Base64
import net.i2p.data.Destination

@Log
abstract class Connection implements Closeable {

	final EventBus eventBus
	final Endpoint endpoint
	final boolean incoming
	final HostCache hostCache
    final TrustService trustService
    final MuWireSettings settings
    	
	private final AtomicBoolean running = new AtomicBoolean()
	private final BlockingQueue messages = new LinkedBlockingQueue()
	private final Thread reader, writer
	
	protected final String name
	
	long lastPingSentTime, lastPongReceivedTime
	
	Connection(EventBus eventBus, Endpoint endpoint, boolean incoming, 
        HostCache hostCache, TrustService trustService, MuWireSettings settings) {
		this.eventBus = eventBus
		this.incoming = incoming
		this.endpoint = endpoint
		this.hostCache = hostCache
        this.trustService = trustService
        this.settings = settings
		
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
        log.info("closing $name")
        endpoint.close()
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
        } catch (Exception e) {
            log.log(Level.WARNING,"unhandled exception in reader",e)
        } finally {
            close()
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
            log.log(Level.WARNING, "unhandled exception in writer",e)
        } finally {
            close()
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
        query.firstHop = e.firstHop
        query.keywords = e.searchEvent.getSearchTerms()
        query.infohash = e.searchEvent.searchHash
        query.replyTo = e.replyTo.toBase64()
        if (e.originator != null)
            query.originator = e.originator.toBase64()
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
        TrustLevel trustLevel = trustService.getLevel(replyTo)
        if (trustLevel == TrustLevel.DISTRUSTED) {
            log.info "dropping search from distrusted peer"
            return
        }
        if (trustLevel == TrustLevel.NEUTRAL && !settings.allowUntrusted()) {
            log.info("dropping search from neutral peer")
            return
        }
        
        Persona originator = null
        if (search.originator != null) {
            originator = new Persona(new ByteArrayInputStream(Base64.decode(search.originator)))
            if (originator.destination != replyTo) {
                log.info("originator doesn't match destination")
                return
            }
        }
        
        
        SearchEvent searchEvent = new SearchEvent(searchTerms : search.keywords,
                                            searchHash : Base64.decode(search.infohash),
                                            uuid : uuid)
        QueryEvent event = new QueryEvent ( searchEvent : searchEvent,
                                            replyTo : replyTo,
                                            originator : originator,
                                            receivedOn : endpoint.destination,
                                            firstHop : search.firstHop )
        eventBus.publish(event)
                                            
    }
}
