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
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.zip.GZIPInputStream

@Log
class BrowseManager {
    
    private static final int BATCH_SIZE = 128
    
    private final I2PConnector connector
    private final EventBus eventBus
    private final Persona me
    
    private final Executor browserThread = Executors.newCachedThreadPool()
    
    BrowseManager(I2PConnector connector, EventBus eventBus, Persona me) {
        this.connector = connector
        this.eventBus = eventBus
        this.me = me
    }
    
    void onUIBrowseEvent(UIBrowseEvent e) {
        browserThread.execute({
            Endpoint endpoint = null
            try {
                eventBus.publish(new BrowseStatusEvent(host : e.host, status : BrowseStatus.CONNECTING, uuid: e.uuid))
                endpoint = connector.connect(e.host.destination)
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
                    if (profileHeader.getPersona() != e.host)
                        throw new IOException("Sender profile mismatch")
                }
                
                // at this stage, start pulling the results
                UUID uuid = e.uuid
                eventBus.publish(new BrowseStatusEvent(host: e.host, status : BrowseStatus.FETCHING, 
                    totalResults : results, uuid : uuid))
                log.info("Starting to fetch $results results with uuid $uuid")
                
                JsonSlurper slurper = new JsonSlurper()
                DataInputStream dis = new DataInputStream(new GZIPInputStream(is))
                UIResultEvent[] batch = new UIResultEvent[Math.min(BATCH_SIZE, results)]
                int j = 0
                for (int i = 0; i < results; i++) {
                    log.fine("parsing result $i at batch position $j")
                    
                    int size = dis.readUnsignedShort()
                    byte [] tmp = new byte[size]
                    dis.readFully(tmp)
                    def json = slurper.parse(tmp)
                    UIResultEvent result = ResultsParser.parse(e.host, uuid, json)
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
                
                eventBus.publish(new BrowseStatusEvent(host: e.host, status : BrowseStatus.FINISHED, uuid : uuid))
                
            } catch (Exception bad) {
                log.log(Level.WARNING, "browse failed", bad)
                eventBus.publish(new BrowseStatusEvent(host: e.host, status : BrowseStatus.FAILED, uuid : e.uuid))
            } finally {
                endpoint?.close()
            }
        } as Runnable)
    }
}
