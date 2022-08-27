package com.muwire.core.search

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
import java.util.logging.Level
import java.util.zip.GZIPInputStream

@Log
class BrowseSession implements Runnable {

    private static final int BATCH_SIZE = 128
    
    private final UIBrowseEvent event
    private final EventBus eventBus
    private final I2PConnector connector
    private final Persona me
    private volatile Thread currentThrad
    private volatile boolean closed
    
    BrowseSession(EventBus eventBus, I2PConnector connector, UIBrowseEvent event, Persona me) {
        this.event = event
        this.eventBus = eventBus
        this.connector = connector
        this.me = me
    }
    
    void run() {
        if (closed)
            return
        currentThrad = Thread.currentThread()
        Endpoint endpoint = null
        try {
            eventBus.publish(new BrowseStatusEvent(host : event.host, status : BrowseStatus.CONNECTING, 
                    uuid: event.uuid, session: this))
            endpoint = connector.connect(event.host.destination)
            OutputStream os = endpoint.getOutputStream()
            os.write("BROWSE\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Persona:${me.toBase64()}\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Path:true\r\n".getBytes(StandardCharsets.US_ASCII))
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

            // at this stage, start pulling the results
            UUID uuid = event.uuid
            eventBus.publish(new BrowseStatusEvent(host: event.host, status : BrowseStatus.FETCHING,
                    totalResults : results, uuid : uuid))
            log.info("Starting to fetch $results results with uuid $uuid")

            JsonSlurper slurper = new JsonSlurper()
            DataInputStream dis = new DataInputStream(new GZIPInputStream(is))
            UIResultEvent[] batch = new UIResultEvent[Math.min(BATCH_SIZE, results)]
            int j = 0
            for (int i = 0; i < results; i++) {
                if (closed)
                    return
                log.fine("parsing result $i at batch position $j")

                int size = dis.readUnsignedShort()
                byte [] tmp = new byte[size]
                dis.readFully(tmp)
                def json = slurper.parse(tmp)
                UIResultEvent result = ResultsParser.parse(event.host, uuid, json)
                result.chat = chat
                result.profileHeader = profileHeader
                batch[j++] = result


                // publish piecemally
                if (j == batch.length) {
                    eventBus.publish(new UIResultBatchEvent(results : batch, uuid : uuid))
                    j = 0
                    batch = new UIResultEvent[Math.min(results - i - 1, BATCH_SIZE)]
                    log.fine("publishing batch, next batch size ${batch.length}")
                }
            }

            eventBus.publish(new BrowseStatusEvent(host: event.host, status : BrowseStatus.FINISHED, uuid : uuid))
        } catch (Exception bad) {
            if (!closed) {
                log.log(Level.WARNING, "browse failed", bad)
                eventBus.publish(new BrowseStatusEvent(host: event.host, status: BrowseStatus.FAILED, uuid: event.uuid))
            }
        } finally {
            currentThrad = null
            endpoint?.close()
        }
    }
    
    void close() {
        log.info("closing browse session for UUID ${event.uuid}")
        closed = true
        currentThrad?.interrupt()
    }
}
