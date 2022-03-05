package com.muwire.core.connection

import com.muwire.core.EventBus
import com.muwire.core.RouterConnectedEvent
import com.muwire.core.RouterDisconnectedEvent
import net.i2p.client.streaming.I2PSocketManager
import net.i2p.client.streaming.I2PSocketManagerFactory
import net.i2p.data.Destination

import java.util.concurrent.Semaphore

class I2PConnector {

    private static final int PERMITS = 4
    
    private final EventBus eventBus
    private final File keyDat
    private final String i2cpHost
    private final int i2cpPort
    private final Properties i2pProperties
    
    volatile I2PSocketManager socketManager
    
    private final Map<Destination, Semaphore> limiter = Collections.synchronizedMap(new WeakHashMap<>())

    I2PConnector() {}

    I2PConnector(EventBus eventBus, File keyDat, String i2cpHost, int i2cpPort, Properties i2pProperties) {
        this.eventBus = eventBus
        this.keyDat = keyDat
        this.i2cpHost = i2cpHost
        this.i2cpPort = i2cpPort
        this.i2pProperties = i2pProperties
    }
    
    synchronized void connect() {
        if (socketManager != null)
            return
        while(true) {
            I2PSocketManager socketManager
            keyDat.withInputStream {
                socketManager = new I2PSocketManagerFactory().createDisconnectedManager(it, i2cpHost, i2cpPort, i2pProperties)
            }
            socketManager.getDefaultOptions().with {
                setReadTimeout(60000)
                setConnectTimeout(15000)
            }
            socketManager.addDisconnectListener({
                this.socketManager = null
                eventBus.publish(new RouterDisconnectedEvent())
            } as I2PSocketManager.DisconnectListener)
        
            def session = socketManager.getSession()
            try {
                session.connect()
                this.socketManager = socketManager
                eventBus.publish(new RouterConnectedEvent(session: session))
            } catch (Exception e) {
                Thread.sleep(1000)
            }
        }
    }
    
    synchronized void shutdown() {
        if (socketManager == null)
            return
        socketManager.destroySocketManager()
        socketManager = null
    }

    Endpoint connect(Destination dest) {
        connect()
        Semaphore limit = limiter.computeIfAbsent(dest, {new Semaphore(PERMITS)})
        limit.acquire()
        try {
            def socket = socketManager.connect(dest)
            return new Endpoint(dest, socket.getInputStream(), socket.getOutputStream(), socket)
        } finally {
            limit.release()
        }
    }

}
