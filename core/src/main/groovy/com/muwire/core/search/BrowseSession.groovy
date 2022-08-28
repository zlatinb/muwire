package com.muwire.core.search

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.profile.MWProfileHeader
import com.muwire.core.util.DataUtil
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Base64

import java.nio.charset.StandardCharsets
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.zip.GZIPInputStream

@Log
class BrowseSession implements Runnable {

    private static final int PING_INTERVAL = 45000
    private static final int BATCH_SIZE = 128
    
    private final UIBrowseEvent event
    private final EventBus eventBus
    private final I2PConnector connector
    private final Persona me
    
    private volatile int version
    
    private volatile Thread currentThread
    private volatile boolean closed
    
    private final BlockingQueue<Request> fetchQueue = new LinkedBlockingDeque<>()

    BrowseSession(EventBus eventBus, I2PConnector connector, UIBrowseEvent event, Persona me) {
        this.event = event
        this.eventBus = eventBus
        this.connector = connector
        this.me = me
    }
    
    void fetch(List<String> path, boolean recursive) {
        fetchQueue.offer(new Request(path, recursive))
    }
    
    boolean supportsIncremental() {
        version == Constants.BROWSE_VERSION
    }
    
    void run() {
        if (closed)
            return
        currentThread = Thread.currentThread()
        Endpoint endpoint = null
        try {
            eventBus.publish(new BrowseStatusEvent(host : event.host, status : BrowseStatus.CONNECTING, 
                    uuid: event.uuid, session: this))
            endpoint = connector.connect(event.host.destination)
            OutputStream os = endpoint.getOutputStream()
            os.write("BROWSE\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Persona:${me.toBase64()}\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Path:true\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Version:${Constants.BROWSE_VERSION}\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("\r\n".getBytes(StandardCharsets.US_ASCII))

            InputStream is = endpoint.getInputStream()
            String code = DataUtil.readTillRN(is)
            if (!code.startsWith("200"))
                throw new IOException("Invalid code $code")

            // parse all headers
            Map<String,String> headers = DataUtil.readAllHeaders(is)

            if (!headers.containsKey("Count"))
                throw new IOException("No count header")

            int results = Integer.parseInt(headers['Count'])

            boolean chat = headers.containsKey("Chat") && Boolean.parseBoolean(headers['Chat'])

            MWProfileHeader profileHeader = null
            if (headers.containsKey("ProfileHeader")) {
                byte [] profileHeaderBytes = Base64.decode(headers['ProfileHeader'])
                profileHeader = new MWProfileHeader(new ByteArrayInputStream(profileHeaderBytes))
                if (profileHeader.getPersona() != event.host)
                    throw new IOException("Sender profile mismatch")
            }
            
            version = 1
            if (headers.containsKey("Version"))
                version = Integer.parseInt(headers['Version'])

            UUID uuid = event.uuid
            // if we're version 1 just pull everything
            if (version == 1) {
                eventBus.publish(new BrowseStatusEvent(host: event.host, status: BrowseStatus.FETCHING,
                        totalResults: results, currentItems: results, uuid: uuid))
                log.info("Starting to fetch $results results with uuid $uuid")

                JsonSlurper slurper = new JsonSlurper()
                DataInputStream dis = new DataInputStream(new GZIPInputStream(is))
                UIResultEvent[] batch = new UIResultEvent[Math.min(BATCH_SIZE, results)]
                int j = 0
                for (int i = 0; i < results; i++) {
                    if (closed)
                        return
                    log.fine("parsing result $i at batch position $j")

                    def json = readJson(slurper, dis)
                    UIResultEvent result = ResultsParser.parse(event.host, uuid, json)
                    result.chat = chat
                    result.profileHeader = profileHeader
                    batch[j++] = result


                    // publish piecemally
                    if (j == batch.length) {
                        eventBus.publish(new UIResultBatchEvent(results: batch, uuid: uuid))
                        j = 0
                        batch = new UIResultEvent[Math.min(results - i - 1, BATCH_SIZE)]
                        log.fine("publishing batch, next batch size ${batch.length}")
                    }
                }
                eventBus.publish(new BrowseStatusEvent(host: event.host, status : BrowseStatus.FINISHED, uuid : uuid))
            } else if (version == 2) {
                Set<Request> fetchedPaths = new HashSet<>()
                while(true) {
                    // version 2 should have Files and Dirs headers
                    if (!headers.containsKey("Files"))
                        throw new Exception("Files header missing")
                    int files = Integer.parseInt(headers['Files'])
                    if (!headers.containsKey("Dirs"))
                        throw new Exception("Dirs header missing")
                    int dirs = Integer.parseInt(headers['Dirs'])
                    eventBus.publish(new BrowseStatusEvent(host: event.host, status: BrowseStatus.FETCHING,
                            totalResults: results, currentItems: (files + dirs), uuid: uuid))
                    log.info("starting to fetch ${files} files and ${dirs} dirs with uuid $uuid")

                    JsonSlurper slurper = new JsonSlurper()
                    DataInputStream dis = new DataInputStream(new GZIPInputStream(is))
                    UIResultEvent[] batch = new UIResultEvent[Math.min(BATCH_SIZE, files)]
                    int j = 0
                    for (int i = 0; i < files; i++) {
                        if (closed)
                            return
                        log.fine("parsing result $i at batch position $j")

                        def json = readJson(slurper, dis)
                        UIResultEvent result = ResultsParser.parse(event.host, uuid, json)
                        result.chat = chat
                        result.profileHeader = profileHeader
                        batch[j++] = result


                        // publish piecemally
                        if (j == batch.length) {
                            eventBus.publish(new UIResultBatchEvent(results: batch, uuid: uuid))
                            j = 0
                            batch = new UIResultEvent[Math.min(results - i - 1, BATCH_SIZE)]
                            log.fine("publishing batch, next batch size ${batch.length}")
                        }
                    }
                    for (int i = 0; i < dirs; i++) {
                        if (closed)
                            return

                        def json = readJson(slurper, dis)
                        if (!json.directory || json.path == null)
                            throw new Exception("Invalid dir json")
                        List<String> path = json.path.collect { DataUtil.readi18nString(Base64.decode(it)) }
                        def event = new UIBrowseDirEvent(uuid: uuid,
                                path: path.toArray(new String[0]))
                        eventBus.publish(event)
                    }
                    eventBus.publish(new BrowseStatusEvent(host: event.host, status : BrowseStatus.FINISHED, uuid : uuid))
                    
                    while(true) {
                        Request nextPath = fetchQueue.poll(PING_INTERVAL, TimeUnit.MILLISECONDS)
                        if (nextPath == null) {
                            log.fine("sending PING on browse connection uuid $uuid")
                            os.write("PING\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
                            os.flush()
                            String pong = DataUtil.readTillRN(is)
                            if (pong != "PONG")
                                throw new Exception("didn't get a pong after a ping")
                            DataUtil.readAllHeaders(is)
                        } else {
                            log.fine("sending GET for path ${nextPath.path} recursive ${nextPath.recursive}")
                             
                            if (!fetchedPaths.add(nextPath))
                                continue
                            os.write("GET ${nextPath.getJoined()}\r\n".getBytes(StandardCharsets.US_ASCII))
                            os.write("Recursive:${nextPath.recursive}\r\n".getBytes(StandardCharsets.US_ASCII))
                            os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
                            os.flush()
                            String response = DataUtil.readTillRN(is)
                            if (!response.startsWith("200"))
                                throw new Exception("response not ok")
                            headers = DataUtil.readAllHeaders(is)
                            break
                        }
                    }
                }
            } else 
                throw new Exception("unrecognized version $version")
        } catch (Exception bad) {
            if (!closed) {
                log.log(Level.WARNING, "browse failed", bad)
                eventBus.publish(new BrowseStatusEvent(host: event.host, status: BrowseStatus.FAILED, uuid: event.uuid))
            }
        } finally {
            currentThread = null
            endpoint?.close()
        }
    }
    
    private static def readJson(JsonSlurper slurper, DataInputStream dis) {
        int size = dis.readUnsignedShort()
        byte[] tmp = new byte[size]
        dis.readFully(tmp)
        slurper.parse(tmp) 
    }
    
    void close() {
        log.info("closing browse session for UUID ${event.uuid}")
        closed = true
        currentThread?.interrupt()
    }
    
    private static class Request {
        private final List<String> path
        private final boolean recursive
        private String joined
        Request(List<String> path, boolean recursive) {
            this.path = path
            this.recursive = recursive
        }
        
        String getJoined() {
            if (joined == null) {
                def encoded = path.collect {Base64.encode(DataUtil.encodei18nString(it))}
                joined = encoded.join(",")
            }
            joined
        }
        
        int hashCode() {
            Objects.hash(getJoined(), recursive)
        }
        
        boolean equals(Object o) {
            if (!(o instanceof Request))
                return false
            Request other = (Request) o
            return getJoined() == other.getJoined() &&
                    recursive == other.recursive
        }
    }
}
