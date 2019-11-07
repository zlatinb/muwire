package com.muwire.core.search

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.util.DataUtil

import groovy.json.JsonSlurper
import groovy.util.logging.Log

import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.zip.GZIPInputStream

@Log
class BrowseManager {
    
    private final I2PConnector connector
    private final EventBus eventBus
    private final Persona me
    
    private final Executor browserThread = Executors.newSingleThreadExecutor()
    
    BrowseManager(I2PConnector connector, EventBus eventBus, Persona me) {
        this.connector = connector
        this.eventBus = eventBus
        this.me = me
    }
    
    void onUIBrowseEvent(UIBrowseEvent e) {
        browserThread.execute({
            Endpoint endpoint = null
            try {
                eventBus.publish(new BrowseStatusEvent(status : BrowseStatus.CONNECTING))
                endpoint = connector.connect(e.host.destination)
                OutputStream os = endpoint.getOutputStream()
                os.write("BROWSE\r\n".getBytes(StandardCharsets.US_ASCII))
                os.write("Persona:${me.toBase64()}\r\n".getBytes(StandardCharsets.US_ASCII))
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
                
                // at this stage, start pulling the results
                eventBus.publish(new BrowseStatusEvent(status : BrowseStatus.FETCHING, totalResults : results))
                
                JsonSlurper slurper = new JsonSlurper()
                DataInputStream dis = new DataInputStream(new GZIPInputStream(is))
                UUID uuid = UUID.randomUUID()
                for (int i = 0; i < results; i++) {
                    int size = dis.readUnsignedShort()
                    byte [] tmp = new byte[size]
                    dis.readFully(tmp)
                    def json = slurper.parse(tmp)
                    UIResultEvent result = ResultsParser.parse(e.host, uuid, json)
                    eventBus.publish(result)
                }
                
                eventBus.publish(new BrowseStatusEvent(status : BrowseStatus.FINISHED))
                
            } catch (Exception bad) {
                log.log(Level.WARNING, "browse failed", bad)
                eventBus.publish(new BrowseStatusEvent(status : BrowseStatus.FAILED))
            } finally {
                endpoint?.close()
            }
        } as Runnable)
    }
}
