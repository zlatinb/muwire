package com.muwire.core.trust

import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.UILoadedEvent
import com.muwire.core.connection.I2PConnector

import net.i2p.data.Destination

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
                    check(trustList, now)
                }
            }
        } catch (InterruptedException e) {
            if (!shutdown)
                throw e
        }
    }
    
    private void check(RemoteTrustList trustList, long now) {
        // TODO: fetch trustlist and update timestamp
    }
}
