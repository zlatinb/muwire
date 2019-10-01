package com.muwire.core.connection

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.logging.Level
import java.util.zip.Deflater
import java.util.zip.DeflaterInputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.hostcache.HostCache
import com.muwire.core.hostcache.HostDiscoveredEvent

import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

@Log
class ConnectionEstablisher {

    private static final int CONCURRENT = 4

    final EventBus eventBus
    final I2PConnector i2pConnector
    final MuWireSettings settings
    final ConnectionManager connectionManager
    final HostCache hostCache

    final Timer timer
    final ExecutorService executor, closer

    final Set inProgress = new ConcurrentHashSet()

    ConnectionEstablisher(){}

    ConnectionEstablisher(EventBus eventBus, I2PConnector i2pConnector, MuWireSettings settings,
        ConnectionManager connectionManager, HostCache hostCache) {
        this.eventBus = eventBus
        this.i2pConnector = i2pConnector
        this.settings = settings
        this.connectionManager = connectionManager
        this.hostCache = hostCache
        timer = new Timer("connection-timer",true)
        executor = Executors.newFixedThreadPool(CONCURRENT, { r ->
            def rv = new Thread(r)
            rv.setDaemon(true)
            rv.setName("connector-${System.currentTimeMillis()}")
            rv
        } as ThreadFactory)
        
        closer = Executors.newSingleThreadExecutor()
    }

    void start() {
        timer.schedule({connectIfNeeded()} as TimerTask, 100, 1000)
    }

    void stop() {
        timer.cancel()
        executor.shutdownNow()
        closer.shutdown()
    }

    private void connectIfNeeded() {
        if (!connectionManager.needsConnections())
            return
        if (inProgress.size() >= CONCURRENT)
            return

        def toTry = null
        for (int i = 0; i < 5; i++) {
            toTry = hostCache.getHosts(1)
            if (toTry.isEmpty())
                return
            toTry = toTry[0]
            if (!connectionManager.isConnected(toTry) &&
                !inProgress.contains(toTry)) {
                break
            }
        }
        if (toTry == null)
            return
        if (!connectionManager.isConnected(toTry) && inProgress.add(toTry))
            executor.execute({connect(toTry)} as Runnable)
    }

    private void connect(Destination toTry) {
        log.info("starting connect to ${toTry.toBase32()}")
        try {
            def endpoint = i2pConnector.connect(toTry)
            log.info("successful transport connect to ${toTry.toBase32()}")

            // outgoing handshake
            endpoint.outputStream.write("MuWire ".bytes)
            def type = settings.isLeaf() ? "leaf" : "peer"
            endpoint.outputStream.write(type.bytes)
            endpoint.outputStream.flush()

            InputStream is = endpoint.inputStream
            int read = is.read()
            if (read == -1) {
                fail endpoint
                return
            }
            switch(read) {
                case (byte)'O': readK(endpoint); break
                case (byte)'R': readEJECT(endpoint); break
                default :
                    log.warning("unknown response $read")
                    fail endpoint
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Couldn't connect to ${toTry.toBase32()}", e)
            def endpoint = new Endpoint(toTry, null, null, null)
            fail(endpoint)
        } finally {
            inProgress.remove(toTry)
        }
    }

    private void fail(Endpoint endpoint) {
        closer.execute {
            endpoint.close()
            eventBus.publish(new ConnectionEvent(endpoint: endpoint, incoming: false, leaf: false, status: ConnectionAttemptStatus.FAILED))
        } as Runnable
    }

    private void readK(Endpoint e) {
        int read = e.inputStream.read()
        if (read != 'K') {
            log.warning("unknown response after O: $read")
            fail e
            return
        }

        log.info("connection to ${e.destination.toBase32()} established")

        // wrap into deflater / inflater streams and publish
        def wrapped = new Endpoint(e.destination, new InflaterInputStream(e.inputStream), new DeflaterOutputStream(e.outputStream, true), e.toClose)
        eventBus.publish(new ConnectionEvent(endpoint: wrapped, incoming: false, leaf: false, status: ConnectionAttemptStatus.SUCCESSFUL))
    }

    private void readEJECT(Endpoint e) {
        byte[] eject = "EJECT".bytes
        for (int i = 0; i < eject.length; i++) {
            int read = e.inputStream.read()
            if (read != eject[i]) {
                log.warning("Unknown response after R at position $i")
                fail e
                return
            }
        }
        log.info("connection to ${e.destination.toBase32()} rejected")


        eventBus.publish(new ConnectionEvent(endpoint: e, incoming: false, leaf: false, status: ConnectionAttemptStatus.REJECTED))
        try {
            DataInputStream dais = new DataInputStream(e.inputStream)
            int payloadSize = dais.readUnsignedShort()
            byte[] payload = new byte[payloadSize]
            dais.readFully(payload)

            def json = new JsonSlurper()
            json = json.parse(payload)

            if (json.tryHosts == null) {
                log.warning("post-rejection json didn't contain hosts to try")
                return
            }

            json.tryHosts.asList().each {
                Destination suggested = new Destination(it)
                eventBus.publish(new HostDiscoveredEvent(destination: suggested))
            }
        } catch (Exception ignore) {
            log.log(Level.WARNING,"Problem parsing post-rejection payload",ignore)
        } finally {
            // the end
            closer.execute({e.close()} as Runnable)
        }
    }

    public boolean isInProgress(Destination d) {
        inProgress.contains(d)
    }
}
