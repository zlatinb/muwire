package com.muwire.core.search

import com.muwire.core.Constants
import com.muwire.core.EventBus
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
    
    private final Executor browserThread = Executors.newSingleThreadExecutor()
    
    BrowseManager(I2PConnector connector, EventBus eventBus) {
        this.connector = connector
        this.eventBus = eventBus
    }
    
    void onUIBrowseEvent(UIBrowseEvent e) {
        browserThread.execute({
            Endpoint endpoint = null
            try {
                eventBus.publish(new BrowseStatusEvent(status : BrowseStatus.CONNECTING))
                endpoint = connector.connect(e.host.destination)
                OutputStream os = endpoint.getOutputStream()
                os.write("BROWSE\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
             
                InputStream is = endpoint.getInputStream()
                String code = DataUtil.readTillRN(is)
                if (!code.startsWith("200"))
                    throw new IOException("Invalid code")
                    
                // parse all headers
                Map<String,String> headers = new HashMap<>()
                String header
                while((header = DataUtil.readTillRN(is)) != "" && headers.size() < Constants.MAX_HEADERS) {
                    int colon = header.indexOf(':')
                    if (colon == -1 || colon == header.length() - 1)
                        throw new IOException("invalid header $header")
                    String key = header.substring(0, colon)
                    String value = header.substring(colon + 1)
                    headers[key] = value.trim()
                }
                
                if (!headers.containsKey("Count"))
                    throw new IOException("No count header")
                
                int results = Integer.parseInt(headers['Count'])
                
                // at this stage, start pulling the results
                eventBus.publish(new BrowseStatusEvent(status : BrowseStatus.FETCHING))
                
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
