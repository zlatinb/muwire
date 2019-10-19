package com.muwire.core.connection

import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.InflaterInputStream

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.files.FileManager
import com.muwire.core.hostcache.HostCache
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService
import com.muwire.core.upload.UploadManager
import com.muwire.core.util.DataUtil
import com.muwire.core.search.InvalidSearchResultException
import com.muwire.core.search.ResultsParser
import com.muwire.core.search.ResultsSender
import com.muwire.core.search.SearchManager
import com.muwire.core.search.UIResultBatchEvent
import com.muwire.core.search.UIResultEvent
import com.muwire.core.search.UnexpectedResultsException

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log

@Log
class ConnectionAcceptor {

    final EventBus eventBus
    final UltrapeerConnectionManager manager
    final MuWireSettings settings
    final I2PAcceptor acceptor
    final HostCache hostCache
    final TrustService trustService
    final SearchManager searchManager
    final UploadManager uploadManager
    final FileManager fileManager
    final ConnectionEstablisher establisher

    final ExecutorService acceptorThread
    final ExecutorService handshakerThreads

    private volatile shutdown

    ConnectionAcceptor(EventBus eventBus, UltrapeerConnectionManager manager,
        MuWireSettings settings, I2PAcceptor acceptor, HostCache hostCache,
        TrustService trustService, SearchManager searchManager, UploadManager uploadManager,
        FileManager fileManager, ConnectionEstablisher establisher) {
        this.eventBus = eventBus
        this.manager = manager
        this.settings = settings
        this.acceptor = acceptor
        this.hostCache = hostCache
        this.trustService = trustService
        this.searchManager = searchManager
        this.fileManager = fileManager
        this.uploadManager = uploadManager
        this.establisher = establisher

        acceptorThread = Executors.newSingleThreadExecutor { r ->
            def rv = new Thread(r)
            rv.setDaemon(true)
            rv.setName("acceptor")
            rv
        }

        handshakerThreads = Executors.newCachedThreadPool { r ->
            def rv = new Thread(r)
            rv.setDaemon(true)
            rv.setName("acceptor-processor-${System.currentTimeMillis()}")
            rv
        }
    }

    void start() {
        acceptorThread.execute({acceptLoop()} as Runnable)
    }

    void stop() {
        shutdown = true
        acceptorThread.shutdownNow()
        handshakerThreads.shutdownNow()
    }

    private void acceptLoop() {
        try {
        while(true) {
            def incoming = acceptor.accept()
            log.info("accepted connection from ${incoming.destination.toBase32()}")
            switch(trustService.getLevel(incoming.destination)) {
                case TrustLevel.TRUSTED : break
                case TrustLevel.NEUTRAL :
                    if (settings.allowUntrusted())
                        break
                case TrustLevel.DISTRUSTED :
                    log.info("Disallowing distrusted connection")
                    incoming.close()
                    continue
            }
            handshakerThreads.execute({processIncoming(incoming)} as Runnable)
        }
        } catch (Exception e) {
            log.log(Level.WARNING, "exception in accept loop",e)
            if (!shutdown)
                throw e
        }
    }

    private void processIncoming(Endpoint e) {
        InputStream is = e.inputStream
        try {
            int read = is.read()
            switch(read) {
                case (byte)'M':
                    if (settings.isLeaf())
                        throw new IOException("Incoming connection as leaf")
                    processMuWire(e)
                    break
                case (byte)'G':
                    processGET(e)
                    break
                case (byte)'H':
                    processHashList(e)
                    break
                case (byte)'P':
                    processPOST(e)
                    break
                case (byte)'T':
                    processTRUST(e)
                    break
                case (byte)'B':
                    processBROWSE(e)
                    break
                default:
                    throw new Exception("Invalid read $read")
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, "incoming connection failed",ex)
            e.close()
            eventBus.publish new ConnectionEvent(endpoint: e, incoming: true, leaf: null, status: ConnectionAttemptStatus.FAILED)
        }
    }

    private void processMuWire(Endpoint e) {
        byte[] uWire = "uWire ".bytes
        for (int i = 0; i < uWire.length; i++) {
            int read = e.inputStream.read()
            if (read != uWire[i]) {
                throw new IOException("unexpected value $read at position $i")
            }
        }

        byte[] type = new byte[4]
        DataInputStream dis = new DataInputStream(e.inputStream)
        dis.readFully(type)

        if (type == "leaf".bytes)
            handleIncoming(e, true)
        else if (type == "peer".bytes)
            handleIncoming(e, false)
        else
            throw new IOException("unknown connection type $type")
    }

    private void handleIncoming(Endpoint e, boolean leaf) {
        boolean accept = !manager.isConnected(e.destination) &&
            !establisher.isInProgress(e.destination) &&
            (leaf ? manager.hasLeafSlots() : manager.hasPeerSlots())
        if (accept) {
            log.info("accepting connection, leaf:$leaf")
            e.outputStream.write("OK".bytes)
            e.outputStream.flush()
            def wrapped = new Endpoint(e.destination, new InflaterInputStream(e.inputStream), new DeflaterOutputStream(e.outputStream, true), e.toClose)
            eventBus.publish(new ConnectionEvent(endpoint: wrapped, incoming: true, leaf: leaf, status: ConnectionAttemptStatus.SUCCESSFUL))
        } else {
            log.info("rejecting connection, leaf:$leaf")
            e.outputStream.write("REJECT".bytes)
            def hosts = hostCache.getGoodHosts(10)
            if (!hosts.isEmpty()) {
                def json = [:]
                json.tryHosts = hosts.collect { d -> d.toBase64() }
                json = JsonOutput.toJson(json)
                def os = new DataOutputStream(e.outputStream)
                os.writeShort(json.bytes.length)
                os.write(json.bytes)
            }
            e.outputStream.flush()
            e.close()
            eventBus.publish(new ConnectionEvent(endpoint: e, incoming: true, leaf: leaf, status: ConnectionAttemptStatus.REJECTED))
        }
    }



    private void processGET(Endpoint e) {
        byte[] et = new byte[3]
        final DataInputStream dis = new DataInputStream(e.getInputStream())
        dis.readFully(et)
        if (et != "ET ".getBytes(StandardCharsets.US_ASCII))
            throw new IOException("Invalid GET connection")
        uploadManager.processGET(e)
    }

    private void processHashList(Endpoint e) {
        byte[] ashList = new byte[8]
        final DataInputStream dis = new DataInputStream(e.getInputStream())
        dis.readFully(ashList)
        if (ashList != "ASHLIST ".getBytes(StandardCharsets.US_ASCII))
            throw new IOException("Invalid HASHLIST connection")
        uploadManager.processHashList(e)
    }

    private void processPOST(final Endpoint e) throws IOException {
        byte [] ost = new byte[4]
        final DataInputStream dis = new DataInputStream(e.getInputStream())
        dis.readFully(ost)
        if (ost != "OST ".getBytes(StandardCharsets.US_ASCII))
            throw new IOException("Invalid POST connection")
        JsonSlurper slurper = new JsonSlurper()
        try {
            byte[] uuid = new byte[36]
            dis.readFully(uuid)
            UUID resultsUUID = UUID.fromString(new String(uuid, StandardCharsets.US_ASCII))
            if (!searchManager.hasLocalSearch(resultsUUID))
                throw new UnexpectedResultsException(resultsUUID.toString())

            byte[] rn = new byte[4]
            dis.readFully(rn)
            if (rn != "\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
                throw new IOException("invalid request header")

            Persona sender = new Persona(dis)
            if (sender.destination != e.getDestination())
                throw new IOException("Sender destination mismatch expected $e.getDestination(), got $sender.destination")
            int nResults = dis.readUnsignedShort()
            UIResultEvent[] results = new UIResultEvent[nResults]
            for (int i = 0; i < nResults; i++) {
                int jsonSize = dis.readUnsignedShort()
                byte [] payload = new byte[jsonSize]
                dis.readFully(payload)
                def json = slurper.parse(payload)
                results[i] = ResultsParser.parse(sender, resultsUUID, json)
            }
            eventBus.publish(new UIResultBatchEvent(uuid: resultsUUID, results: results))
        } catch (IOException | UnexpectedResultsException | InvalidSearchResultException bad) {
            log.log(Level.WARNING, "failed to process POST", bad)
        } finally {
            e.close()
        }
    }
    
    private void processBROWSE(Endpoint e) {
        try {
            byte [] rowse = new byte[7]
            DataInputStream dis = new DataInputStream(e.getInputStream())
            dis.readFully(rowse)
            if (rowse != "ROWSE\r\n".getBytes(StandardCharsets.US_ASCII))
                throw new IOException("Invalid BROWSE connection")
            String header
            while ((header = DataUtil.readTillRN(dis)) != ""); // ignore headers for now

            OutputStream os = e.getOutputStream()
            if (!settings.browseFiles) {
                os.write("403 Not Allowed\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
                os.flush()
                e.close()
                return
            }


            os.write("200 OK\r\n".getBytes(StandardCharsets.US_ASCII))

            def sharedFiles = fileManager.getSharedFiles().values()

            os.write("Count: ${sharedFiles.size()}\r\n\r\n".getBytes(StandardCharsets.US_ASCII))

            DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(os))
            JsonOutput jsonOutput = new JsonOutput()
            sharedFiles.each {
                def obj = ResultsSender.sharedFileToObj(it, false)
                def json = jsonOutput.toJson(obj)
                dos.writeShort((short)json.length())
                dos.write(json.getBytes(StandardCharsets.US_ASCII))
            }
            dos.flush()
            dos.close()
        } finally {
            e.close()
        }
    }

    private void processTRUST(Endpoint e) {
        try {
            byte[] RUST = new byte[6]
            DataInputStream dis = new DataInputStream(e.getInputStream())
            dis.readFully(RUST)
            if (RUST != "RUST\r\n".getBytes(StandardCharsets.US_ASCII))
                throw new IOException("Invalid TRUST connection")
            String header
            while ((header = DataUtil.readTillRN(dis)) != ""); // ignore headers for now

                OutputStream os = e.getOutputStream()
            if (!settings.allowTrustLists) {
                os.write("403 Not Allowed\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
                os.flush()
                e.close()
                return
            }

            os.write("200 OK\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
            List<Persona> good = new ArrayList<>(trustService.good.values())
            int size = Math.min(Short.MAX_VALUE * 2, good.size())
            good = good.subList(0, size)
            DataOutputStream dos = new DataOutputStream(os)
            dos.writeShort(size)
            good.each {
                it.write(dos)
            }

            List<Persona> bad = new ArrayList<>(trustService.bad.values())
            size = Math.min(Short.MAX_VALUE * 2, bad.size())
            bad = bad.subList(0, size)
            dos.writeShort(size)
            bad.each {
                it.write(dos)
            }

            dos.flush()
        } finally {
            e.close()
        }
    }

}
