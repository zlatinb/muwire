package com.muwire.core.trust

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.UILoadedEvent
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.util.DataUtil

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
class TrustSubscriber {
    private final EventBus eventBus
    private final I2PConnector i2pConnector
    private final MuWireSettings settings
    
    private final Map<Destination, RemoteTrustList> remoteTrustLists = new ConcurrentHashMap<>()

    private final Object waitLock = new Object()
    private volatile boolean shutdown
    private volatile Thread thread    
    
    TrustSubscriber(EventBus eventBus, I2PConnector i2pConnector, MuWireSettings settings) {
        this.eventBus = eventBus
        this.i2pConnector = i2pConnector
        this.settings = settings
    }
    
    void onUILoadedEvent(UILoadedEvent e) {
        thread = new Thread({checkLoop()} as Runnable, "trust-subscriber")
        thread.setDaemon(true)
        thread.start()
    }
    
    void stop() {
        shutdown = true
        thread?.interrupt()
    }
    
    void onTrustSubscriptionEvent(TrustSubscriptionEvent e) {
        if (!e.subscribe) {
            settings.trustSubscriptions.remove(e.persona)
            remoteTrustLists.remove(e.persona.destination)
        } else {
            settings.trustSubscriptions.add(e.persona)
            RemoteTrustList trustList = remoteTrustLists.putIfAbsent(e.persona.destination, new RemoteTrustList(e.persona))
            trustList.timestamp = 0
            synchronized(waitLock) {
                waitLock.notify()
            }
        }
    }
    
    private void checkLoop() {
        try {
            while(!shutdown) {
                synchronized(waitLock) {
                    waitLock.wait(60 * 1000)
                }
                final long now = System.currentTimeMillis()
                remoteTrustLists.values().each { trustList ->
                    if (now - trustList.timestamp < settings.trustListInterval * 60 * 60 * 1000)
                        return
                    trustList.status = RemoteTrustList.Status.UPDATING
                    eventBus.publish(new TrustSubscriptionUpdatedEvent(trustList : trustList))
                    check(trustList, now)
                    trustList.status = RemoteTrustList.Status.UPDATED
                    eventBus.publish(new TrustSubscriptionUpdatedEvent(trustList : trustList))
                }
            }
        } catch (InterruptedException e) {
            if (!shutdown)
                throw e
        }
    }
    
    private void check(RemoteTrustList trustList, long now) {
        log.info("fetching trust list from ${trustList.persona.getHumanReadableName()}")
        Endpoint endpoint = null
        try {
            endpoint = i2pConnector.connect(trustList.persona.destination)
            OutputStream os = endpoint.getOutputStream()
            InputStream is = endpoint.getInputStream()
            os.write("TRUST\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
            os.flush()
            
            String codeString = DataUtil.readTillRN(is)
            int space = codeString.indexOf(' ')
            if (space > 0)
                codeString = codeString.substring(0,space)
            int code = Integer.parseInt(codeString.trim())
            
            if (code != 200) {
                log.info("couldn't fetch trust list, code $code")
                return
            }
            
            DataInputStream dis = new DataInputStream(is)
            
            Set<Persona> good = new HashSet<>()
            int nGood = dis.readUnsignedShort()
            for (int i = 0; i < nGood; i++) {
                Persona p = new Persona(dis)
                good.add(p)
            }
            
            Set<Persona> bad = new HashSet<>()
            int nBad = dis.readUnsignedShort()
            for (int i = 0; i < nBad; i++) {
                Persona p = new Persona(dis)
                bad.add(p)
            }
            
            trustList.timestamp = now
            trustList.good.clear()
            trustList.good.addAll(good)
            trustList.bad.clear()
            trustList.bad.addAll(bad)
            
        } catch (Exception e) {
            log.log(Level.WARNING,"exception fetching trust list from ${trustList.persona.getHumanReadableName()}",e)
        } finally {
            endpoint?.close()
        }
        
    }
}
