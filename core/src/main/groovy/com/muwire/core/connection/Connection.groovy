package com.muwire.core.connection

import java.nio.charset.StandardCharsets
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

import com.muwire.core.Constants
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
import net.i2p.crypto.DSAEngine
import net.i2p.data.Base64
import net.i2p.data.Destination
import net.i2p.data.Signature

@Log
abstract class Connection implements Closeable {
    
    private static final int SEARCHES = 10
    private static final long INTERVAL = 1000
    
    private static final MAX_PONGS_V1 = 2
    private static final MAX_PONGS_V2 = 4

    final EventBus eventBus
    final Endpoint endpoint
    final boolean incoming
    final HostCache hostCache
    final TrustService trustService
    final MuWireSettings settings

    private final AtomicBoolean running = new AtomicBoolean()
    private final BlockingQueue messages = new LinkedBlockingQueue()
    private final Thread reader, writer
    private final LinkedList<Long> searchTimestamps = new LinkedList<>()

    protected final String name

    long lastPingSentTime
    
    private volatile UUID lastPingUUID

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
        reader.interrupt()
        writer.interrupt()
        endpoint.close()
        log.info("closed $name")
        eventBus.publish(new DisconnectionEvent(destination: endpoint.destination))
    }

    protected void readLoop() {
        try {
            while(running.get()) {
                read()
            }
        } catch (InterruptedException ok) {
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
        } catch (InterruptedException ok) {
        } catch (Exception e) {
            log.log(Level.WARNING, "unhandled exception in writer",e)
        } finally {
            close()
        }
    }

    protected abstract void write(def message);

    void sendPing(boolean expectResponse) {
        def ping = [:]
        ping.type = "Ping"
        ping.version = 2
        if (expectResponse) {
            lastPingUUID = UUID.randomUUID()
            ping.uuid = lastPingUUID.toString()
        }
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
        query.oobInfohash = e.searchEvent.oobInfohash
        query.searchComments = e.searchEvent.searchComments
        query.compressedResults = e.searchEvent.compressedResults
        query.collections = e.searchEvent.collections
        if (e.searchEvent.searchHash != null)
            query.infohash = Base64.encode(e.searchEvent.searchHash)
        query.replyTo = e.replyTo.toBase64()
        if (e.originator != null)
            query.originator = e.originator.toBase64()
        if (e.sig != null)
            query.sig = Base64.encode(e.sig)
        if (e.queryTime > 0)
            query.queryTime = e.queryTime
        if (e.sig2 != null)
            query.sig2 = Base64.encode(e.sig2)
        messages.put(query)
    }

    protected void handlePing(def ping) {
        log.fine("$name received ping version ${ping.version}")
        if (ping.uuid == null) {
            log.fine("Not responding as there was no UUID")
            return
        }
        if (ping.version < 2)
            handlePingV1(ping)
        else
            handlePingV2(ping)
    }
    
    private void handlePingV1(def ping) {
        def pong = [:]
        pong.type = "Pong"
        pong.version = 1
        pong.uuid = ping.uuid
        pong.pongs = hostCache.getGoodHosts(MAX_PONGS_V1).collect { d -> d.toBase64() }
        messages.put(pong)
    }
    
    private void handlePingV2(def ping) {
        UUID uuid = UUID.fromString(ping.uuid)
        byte [] pongPayload = MessageUtil.createPongV2(uuid, hostCache.getGoodHosts(MAX_PONGS_V2))
        messages.put(pongPayload)
    }

    protected void handlePong(def pong) {
        log.fine("$name received pong version ${pong.version}")
        if (pong.pongs == null)
            throw new Exception("Pong doesn't have pongs")
            
        if (lastPingUUID == null) {
            log.fine "$name received an unexpected pong"
            return
        }
        if (pong.uuid == null) {
            log.fine "$name pong doesn't have uuid"
            return
        }
        UUID pongUUID = UUID.fromString(pong.uuid)
        if (pongUUID != lastPingUUID) {
            log.fine "$name ping/pong uuid mismatch"
            return
        }
        lastPingUUID = null
        
        int limit = pong.version == 1 ? MAX_PONGS_V1 : MAX_PONGS_V2
        pong.pongs.stream().limit(limit).forEach {
            def dest = new Destination(it)
            eventBus.publish(new HostDiscoveredEvent(destination: dest))
        }
    }

    private boolean throttleSearch() {
        final long now = System.currentTimeMillis()
        if (searchTimestamps.size() < SEARCHES) {
            searchTimestamps.addLast(now)
            return false
        }
        Long oldest = searchTimestamps.getFirst()
        if (now - oldest.longValue() < INTERVAL)
            return true
        searchTimestamps.addLast(now)
        searchTimestamps.removeFirst()
        false
    }

    protected void handleSearch(def search) {
        if (throttleSearch()) {
            log.info("dropping excessive search")
            return
        }
        UUID uuid = UUID.fromString(search.uuid)
        byte [] infohash = null
        if (search.infohash != null) {
            search.keywords = null
            infohash = Base64.decode(search.infohash)
        }

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

        boolean oob = false
        if (search.oobInfohash != null)
            oob = search.oobInfohash
        boolean searchComments = false
        if (search.searchComments != null)
            searchComments = search.searchComments
        boolean compressedResults = false
        if (search.compressedResults != null)
            compressedResults = search.compressedResults
        boolean collections
        if (search.collections != null)
            collections = search.collections
        byte[] sig = null
        if (search.sig != null) {
            sig = Base64.decode(search.sig)
            byte [] payload 
            if (infohash != null)
                payload = infohash
            else 
                payload =  String.join(" ",search.keywords).getBytes(StandardCharsets.UTF_8)
            def spk = originator.destination.getSigningPublicKey()
            def signature = new Signature(spk.getType(), sig)
            if (!DSAEngine.getInstance().verifySignature(signature, payload, spk)) {
                log.info("signature didn't match keywords")
                return
            } else
                log.info("query signature verified")
        } else {
            log.info("no signature in query")
            return
        }
        
        byte[] sig2 = null        
        long queryTime = 0
        if (search.sig2 != null) {
            if (search.queryTime == null) {
                log.info("extended signature but no timestamp")
                return
            }
            sig2 = Base64.decode(search.sig2)
            queryTime = search.queryTime
            byte [] payload = (search.uuid + String.valueOf(queryTime)).getBytes(StandardCharsets.US_ASCII)
            def spk = originator.destination.getSigningPublicKey()
            def signature = new Signature(spk.getType(), sig2)
            if (!DSAEngine.getInstance().verifySignature(signature, payload, spk)) {
                log.info("extended signature didn't match uuid and timestamp")
                return
            } else {
                log.info("extended query signature verified")
                if (queryTime < System.currentTimeMillis() - Constants.MAX_QUERY_AGE) {
                    log.info("query too old")
                    return
                }
            }
        } else {
            log.info("no extended signature in query")
            return
        }

        SearchEvent searchEvent = new SearchEvent(searchTerms : search.keywords,
                                            searchHash : infohash,
                                            uuid : uuid,
                                            oobInfohash : oob,
                                            searchComments : searchComments,
                                            compressedResults : compressedResults,
                                            collections : collections,
                                            persona : originator)
        QueryEvent event = new QueryEvent ( searchEvent : searchEvent,
                                            replyTo : replyTo,
                                            originator : originator,
                                            receivedOn : endpoint.destination,
                                            firstHop : search.firstHop,
                                            sig : sig,
                                            queryTime : queryTime,
                                            sig2 : sig2 )
        eventBus.publish(event)

    }
}
