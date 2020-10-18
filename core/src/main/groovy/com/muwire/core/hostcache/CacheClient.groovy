package com.muwire.core.hostcache

import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.connection.ConnectionManager
import com.muwire.core.connection.UltrapeerConnectionManager

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.client.I2PSession
import net.i2p.client.I2PSessionMuxedListener
import net.i2p.client.SendMessageOptions
import net.i2p.client.datagram.I2PDatagramDissector
import net.i2p.client.datagram.I2PDatagramMaker
import net.i2p.data.Destination

@Log
class CacheClient {

    private static final int CRAWLER_RETURN = 10

    final EventBus eventBus
    final HostCache cache
    final ConnectionManager manager
    final I2PSession session
    final long interval
    final MuWireSettings settings
    final Timer timer
    private final AtomicBoolean stopped = new AtomicBoolean();

    public CacheClient(EventBus eventBus, HostCache cache,
        ConnectionManager manager, I2PSession session,
        MuWireSettings settings, long interval) {
        this.eventBus = eventBus
        this.cache = cache
        this.manager = manager
        this.session = session
        this.settings = settings
        this.interval = interval
        this.timer = new Timer("hostcache-client",true)
    }

    void start() {
        session.addMuxedSessionListener(new Listener(), I2PSession.PROTO_DATAGRAM, 0)
        timer.schedule({queryIfNeeded()} as TimerTask, 1, interval)
    }

    void stop() {
        timer.cancel()
        stopped.set(true)
    }

    private void queryIfNeeded() {
        if (stopped.get())
            return
        if (!manager.getConnections().isEmpty())
            return
        if (!cache.getHosts(1, {true} as Predicate).isEmpty()) 
            return

        log.info "Will query hostcaches"

        def ping = [type: "Ping", version: 1, leaf: settings.isLeaf()]
        ping = JsonOutput.toJson(ping)
        def maker = new I2PDatagramMaker(session)
        ping = maker.makeI2PDatagram(ping.bytes)
        def options = new SendMessageOptions()
        options.setSendLeaseSet(true)
        CacheServers.getCacheServers().each {
            log.info "Querying hostcache ${it.toBase32()}"
            try {
                session.sendMessage(it, ping, 0, ping.length, I2PSession.PROTO_DATAGRAM, 1, 0, options)
            } catch (Exception e) {
                if (!stopped.get())
                    throw e
            }
        }
    }

    class Listener implements I2PSessionMuxedListener {

        private final JsonSlurper slurper = new JsonSlurper()

        @Override
        public void messageAvailable(I2PSession session, int msgId, long size) {
        }

        @Override
        public void messageAvailable(I2PSession session, int msgId, long size, int proto, int fromport, int toport) {

            if (proto != I2PSession.PROTO_DATAGRAM) {
                log.warning "Received unexpected protocol $proto"
                return
            }

            def payload = session.receiveMessage(msgId)
            def dissector = new I2PDatagramDissector()
            try {
                dissector.loadI2PDatagram(payload)
                def sender = dissector.getSender()
                log.info("Received something from ${sender.toBase32()}")

                payload = dissector.getPayload()
                payload = slurper.parse(payload)

                if (payload.type == null) {
                    log.warning("type missing")
                    return
                }

                switch(payload.type) {
                    case "Pong" : handlePong(sender, payload); break
                    case "CrawlerPing": handleCrawlerPing(session, sender, payload); break
                    default : log.warning("unknown type ${payload.type}")
                }
            } catch (Exception e) {
                log.warning("Invalid datagram $e")
            }
        }

        @Override
        public void reportAbuse(I2PSession session, int severity) {
        }

        @Override
        public void disconnected(I2PSession session) {
            if (!stopped.get())
                log.severe "Cache client I2P session disconnected"
        }

        @Override
        public void errorOccurred(I2PSession session, String message, Throwable error) {
            log.severe "I2P error occured $message $error"
        }

    }

    private void handlePong(Destination from, def pong) {
        if (!CacheServers.isRegistered(from)) {
            log.warning("received pong from non-registered destination")
            return
        }

        if (pong.pongs == null) {
            log.warning("malformed pong - no pongs")
            return
        }

        pong.pongs.asList().each {
            Destination dest = new Destination(it)
            if (!session.getMyDestination().equals(dest))
                eventBus.publish(new HostDiscoveredEvent(destination: dest, fromHostcache : true))
        }

    }

    private void handleCrawlerPing(I2PSession session, Destination from, def ping) {
        if (settings.isLeaf()) {
            log.warning("Received crawler ping but I'm a leaf")
            return
        }

        switch(settings.getCrawlerResponse()) {
            case CrawlerResponse.NONE:
                log.info("Responding to crawlers is disabled by user")
                break
            case CrawlerResponse.ALL:
                respondToCrawler(session, from, ping)
                break;
            case CrawlerResponse.REGISTERED:
                if (CacheServers.isRegistered(from))
                    respondToCrawler(session, from, ping)
                else
                    log.warning("Ignoring crawler ping from non-registered crawler")
                break
        }
    }

    private void respondToCrawler(I2PSession session, Destination from, def ping) {
        log.info "responding to crawler ping"

        def neighbors = manager.getConnections().collect { c -> c.endpoint.destination.toBase64() }
        Collections.shuffle(neighbors)
        if (neighbors.size() > CRAWLER_RETURN)
            neighbors = neighbors[0..CRAWLER_RETURN - 1]

        def upManager = (UltrapeerConnectionManager) manager;
        def pong = [:]
        pong.peers = neighbors
        pong.uuid = ping.uuid
        pong.type = "CrawlerPong"
        pong.version = 1
        pong.leafSlots = upManager.hasLeafSlots()
        pong.peerSlots = upManager.hasPeerSlots()
        pong = JsonOutput.toJson(pong)

        def maker = new I2PDatagramMaker(session)
        pong = maker.makeI2PDatagram(pong.bytes)
        session.sendMessage(from, pong, I2PSession.PROTO_DATAGRAM, 0, 0)
    }

}
