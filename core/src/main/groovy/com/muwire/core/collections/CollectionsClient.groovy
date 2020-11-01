package com.muwire.core.collections

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.zip.GZIPInputStream

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.util.DataUtil

import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Base64

@Log
class CollectionsClient {

    private final I2PConnector connector
    private final EventBus eventBus
    private final Persona me
    
    private final Executor fetcher = Executors.newCachedThreadPool()
    
    public CollectionsClient(I2PConnector connector, EventBus eventBus, Persona me) {
        this.connector = connector
        this.eventBus = eventBus
        this.me = me
    }
    
    void onUICollectionFetchEvent(UICollectionFetchEvent e) {
        fetcher.execute({
            Endpoint endpoint = null
            try {
                eventBus.publish(new CollectionFetchStatusEvent(status : CollectionFetchStatus.CONNECTING, uuid : e.uuid))
                endpoint = connector.connect(e.host.destination)
                OutputStream os = endpoint.getOutputStream()
                
                String infoHashes = String.join(",", e.infoHashes.collect { Base64.encode(it.getRoot()) })
                os.write("METAFILE ${infoHashes}\r\n".getBytes(StandardCharsets.US_ASCII))
                os.write("Persona:${me.toBase64()}\r\n".getBytes(StandardCharsets.US_ASCII))
                os.write("Version:1\r\n".getBytes(StandardCharsets.US_ASCII))
                os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
                os.flush()
                
                InputStream is = endpoint.getInputStream()
                String code = DataUtil.readTillRN(is)
                if (!code.startsWith("200"))
                    throw new Exception("invalid code $code")
                
                // parse all headers
                Map<String,String> headers = DataUtil.readAllHeaders(is)
                
                if (headers['Version'] != "1")
                    throw new Exception("unknown versio ${headers['Version']}")

                if (!headers.containsKey("Count"))
                    throw new IOException("No count header")
                
                int count = Integer.parseInt(headers['Count'])
                eventBus.publish(new CollectionFetchStatusEvent(status : CollectionFetchStatus.FETCHING, uuid : e.uuid, count : count))
                
                log.info("starting to fetch $count collections")
                
                DataInputStream dis = new DataInputStream(new GZIPInputStream(is))
                MessageDigest digester = MessageDigest.getInstance("SHA-256")
                count.times { 
                    byte[] infoHashBytes = new byte[InfoHash.SIZE]
                    dis.readFully(infoHashBytes)
                    FileCollection collection = new FileCollection(dis)
                    def baos = new ByteArrayOutputStream()
                    collection.write(baos)
                    digester.update(baos.toByteArray())
                    if(!Arrays.equals(infoHashBytes, digester.digest()))
                        throw new Exception("collection infohash did not match")
                    eventBus.publish(new CollectionFetchedEvent(collection : collection, infoHash : new InfoHash(infoHashBytes), uuid : e.uuid))
                }
                eventBus.publish(new CollectionFetchStatusEvent(status : CollectionFetchStatus.FINISHED, uuid : e.uuid))
                
            } catch (Exception bad) {
                log.log(Level.WARNING, "collection fetch failed", bad)
                eventBus.publish(new CollectionFetchStatusEvent(status : CollectionFetchStatus.FAILED, uuid : e.uuid))
            } finally {
                endpoint?.close()
            }
        } as Runnable)
    }
}
