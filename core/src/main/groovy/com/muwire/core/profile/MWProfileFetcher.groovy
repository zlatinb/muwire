package com.muwire.core.profile

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.util.DataUtil
import groovy.util.logging.Log

import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.Supplier
import java.util.logging.Level

@Log
class MWProfileFetcher {
    
    private final I2PConnector connector
    private final EventBus eventBus
    private final Persona me
    private final Supplier<MWProfileHeader> myProfileHeader
    
    private final Executor fetcherThread = Executors.newCachedThreadPool()
    
    MWProfileFetcher(I2PConnector connector, EventBus eventBus, 
                     Persona me, Supplier<MWProfileHeader> myProfileHeader) {
        this.connector = connector
        this.eventBus = eventBus
        this.me = me
        this.myProfileHeader = myProfileHeader
    }
    
    void onUIProfileFetchEvent(UIProfileFetchEvent e) {
        fetcherThread.execute({
            Endpoint endpoint = null
            try {
                eventBus.publish(new MWProfileFetchEvent(host: e.host, status: MWProfileFetchStatus.CONNECTING, uuid: e.uuid))
                endpoint = connector.connect(e.host.destination)
                
                
                OutputStream os = endpoint.getOutputStream()
                os.write("AVATAR\r\n".getBytes(StandardCharsets.US_ASCII))
                os.write("Version:1\r\n".getBytes(StandardCharsets.US_ASCII))
                os.write("Persona:${me.toBase64()}\r\n".getBytes(StandardCharsets.US_ASCII))
                MWProfileHeader header = myProfileHeader.get()
                if (header != null)
                    os.write("ProfileHeader:${header.toBase64()}\r\n".getBytes(StandardCharsets.US_ASCII))
                os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
                
                InputStream is = endpoint.getInputStream()
                String code = DataUtil.readTillRN(is)
                if (!code.startsWith("200"))
                    throw new IOException("invalid code $code")
                
                Map<String,String> headers = DataUtil.readAllHeaders(is)
                
                if (!headers.containsKey("Length"))
                    throw new IOException("No length header")
                
                int length = Integer.parseInt(headers['Length'])
                if (length > Constants.MAX_PROFILE_LENGTH)
                    throw new IOException("profile too large $length")
                
                eventBus.publish(new MWProfileFetchEvent(host: e.host, status: MWProfileFetchStatus.FETCHING, uuid: e.uuid))
                byte[] payload = new byte[length]
                DataInputStream dis = new DataInputStream(is)
                dis.readFully(payload)
                MWProfile profile = new MWProfile(new ByteArrayInputStream(payload))
                if (profile.getHeader().getPersona() != e.host)
                    throw new Exception("profile and host mismatch")
                eventBus.publish(new MWProfileFetchEvent(host: e.host, status: MWProfileFetchStatus.FINISHED,
                    uuid: e.uuid, profile: profile))
            } catch (Exception bad) {
                log.log(Level.WARNING, "profile fetch failed", bad)
                eventBus.publish(new MWProfileFetchEvent(host: e.host, status: MWProfileFetchStatus.FAILED, uuid: e.uuid))
            } finally {
                endpoint?.close()
            }
        } as Runnable)
    }
}
