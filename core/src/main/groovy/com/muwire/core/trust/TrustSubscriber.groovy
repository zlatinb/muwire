package com.muwire.core.trust

import com.muwire.core.profile.MWProfileHeader

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Level

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.UILoadedEvent
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.trust.TrustService.TrustEntry
import com.muwire.core.util.DataUtil

import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Base64
import net.i2p.data.Destination

@Log
class TrustSubscriber {
    private final EventBus eventBus
    private final I2PConnector i2pConnector
    private final MuWireSettings settings

    final Map<Destination, RemoteTrustList> remoteTrustLists = new ConcurrentHashMap<>()

    private final Object waitLock = new Object()
    private volatile boolean shutdown
    private volatile Thread thread
    private final ExecutorService updateThreads = Executors.newCachedThreadPool()

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
        updateThreads.shutdownNow()
    }
    
    void onTrustSubscriptionEvent(TrustSubscriptionEvent e) {
        if (!e.subscribe) {
            remoteTrustLists.remove(e.persona.destination)
        } else {
            RemoteTrustList trustList = remoteTrustLists.putIfAbsent(e.persona.destination, new RemoteTrustList(e.persona))
            trustList?.forceUpdate = true
            synchronized(waitLock) {
                waitLock.notify()
            }
        }
    }
    
    public boolean isSubscribed(Persona p) {
        remoteTrustLists.containsKey(p.destination)
    }

    private void checkLoop() {
        try {
            while(!shutdown) {
                synchronized(waitLock) {
                    waitLock.wait(60 * 1000)
                }
                final long now = System.currentTimeMillis()
                remoteTrustLists.values().each { trustList ->
                    if (trustList.status == RemoteTrustList.Status.UPDATING)
                        return
                    if (!trustList.forceUpdate &&
                        now - trustList.timestamp < settings.trustListInterval * 60 * 60 * 1000)
                        return
                    trustList.forceUpdate = false
                    updateThreads.submit(new UpdateJob(trustList))
                }
            }
        } catch (InterruptedException e) {
            if (!shutdown)
                throw e
        }
    }

    private class UpdateJob implements Runnable {

        private final RemoteTrustList trustList

        UpdateJob(RemoteTrustList trustList) {
            this.trustList = trustList
        }

        public void run() {
            trustList.status = RemoteTrustList.Status.UPDATING
            eventBus.publish(new TrustSubscriptionUpdatedEvent(trustList : trustList))
            if (check(trustList, System.currentTimeMillis()))
                trustList.status = RemoteTrustList.Status.UPDATED
            else
                trustList.status = RemoteTrustList.Status.UPDATE_FAILED
            eventBus.publish(new TrustSubscriptionUpdatedEvent(trustList : trustList))
        }
    }

    private boolean check(RemoteTrustList trustList, long now) {
        log.info("fetching trust list from ${trustList.persona.getHumanReadableName()}")
        Endpoint endpoint = null
        try {
            endpoint = i2pConnector.connect(trustList.persona.destination)
            OutputStream os = endpoint.getOutputStream()
            InputStream is = endpoint.getInputStream()
            os.write("TRUST\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Json:true\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
            os.flush()

            String codeString = DataUtil.readTillRN(is)
            int space = codeString.indexOf(' ')
            if (space > 0)
                codeString = codeString.substring(0,space)
            int code = Integer.parseInt(codeString.trim())

            if (code != 200) {
                log.info("couldn't fetch trust list, code $code")
                return false
            }

            Map<String,String> headers = DataUtil.readAllHeaders(is)
            DataInputStream dis = new DataInputStream(is)
            Set<TrustService.TrustEntry> good = new HashSet<>()
            Set<TrustService.TrustEntry> bad = new HashSet<>()
            
            if (headers.containsKey('Json') && Boolean.parseBoolean(headers['Json'])) {
                int countGood = Integer.parseInt(headers['Good'])
                int countBad = Integer.parseInt(headers['Bad'])
                
                JsonSlurper slurper = new JsonSlurper()
                
                for (int i = 0; i < countGood; i++) {
                    good.add(fromDIS(slurper, dis))
                }
                
                for (int i = 0; i < countBad; i++) {
                    bad.add(fromDIS(slurper, dis))
                }
                
            } else {
                int nGood = dis.readUnsignedShort()
                for (int i = 0; i < nGood; i++) {
                    Persona p = new Persona(dis)
                    good.add(new TrustEntry(p,null,null))
                }

                int nBad = dis.readUnsignedShort()
                for (int i = 0; i < nBad; i++) {
                    Persona p = new Persona(dis)
                    bad.add(new TrustEntry(p, null, null))
                }
            }

            trustList.timestamp = now
            trustList.good.clear()
            trustList.good.addAll(good)
            trustList.bad.clear()
            trustList.bad.addAll(bad)

            return true
        } catch (Exception e) {
            log.log(Level.WARNING,"exception fetching trust list from ${trustList.persona.getHumanReadableName()}",e)
            return false
        } finally {
            endpoint?.close()
        }

    }
    
    private static TrustEntry fromDIS(JsonSlurper slurper, DataInputStream dis) {
        int length = dis.readUnsignedShort()
        byte [] payload = new byte[length]
        dis.readFully(payload)
        def json = slurper.parse(payload)
        byte [] decoded = Base64.decode(json.persona)
        Persona persona = new Persona(new ByteArrayInputStream(decoded))
        MWProfileHeader profileHeader = null
        if (json.profileHeader != null) {
            decoded = Base64.decode(json.profileHeader)
            profileHeader = new MWProfileHeader(new ByteArrayInputStream(decoded))
            if (persona != profileHeader.getPersona())
                throw new IOException("persona and profile mismatch")
        }
        new TrustEntry(persona, (String)json.reason, profileHeader)
    }
}
