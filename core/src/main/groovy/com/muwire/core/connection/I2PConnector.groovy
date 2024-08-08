package com.muwire.core.connection

import com.muwire.core.EventBus
import com.muwire.core.RouterConnectedEvent
import com.muwire.core.RouterDisconnectedEvent
import net.i2p.client.streaming.I2PSocketManager
import net.i2p.client.streaming.I2PSocketManagerFactory
import net.i2p.data.Destination
import net.i2p.I2PException

import java.util.concurrent.Semaphore

class I2PConnector {

    private static final int PERMITS = 4
    
    private final EventBus eventBus
    private final File keyDat
    private final String i2cpHost
    private final int i2cpPort
    private final Properties i2pProperties
    
    private final I2PSocketManager socketManager
    private volatile boolean connected
    
    private Thread connectorThread

    private final Map<Destination, Semaphore> limiter = Collections.synchronizedMap(new WeakHashMap<>())

    I2PConnector() {}

    I2PConnector(EventBus eventBus, File keyDat, String i2cpHost, int i2cpPort, Properties i2pProperties) {
        this.eventBus = eventBus
        this.keyDat = keyDat
        this.i2cpHost = i2cpHost
        this.i2cpPort = i2cpPort
        this.i2pProperties = i2pProperties

        I2PSocketManager socketManager
        keyDat.withInputStream {
            socketManager = new I2PSocketManagerFactory().createDisconnectedManager(it, i2cpHost, i2cpPort, i2pProperties)
        }
        socketManager.getDefaultOptions().with {
            setReadTimeout(60000)
            setConnectTimeout(15000)
        }
        socketManager.addDisconnectListener({
            connected = false
            eventBus.publish(new RouterDisconnectedEvent())
        } as I2PSocketManager.DisconnectListener)
        this.socketManager = socketManager
    }
    
    private void connectI2CP() {
        
        def session = socketManager.getSession()
        session.connect()

        connected = true
        eventBus.publish(new RouterConnectedEvent(session: session,
            socketManager: socketManager))
    }

    void start() {
        connectI2CP()
        Runnable r = {
            while(true) {
                try {
                    if (!connected)
                        connectI2CP()
                } catch (Exception ignored) {}
                finally {
                    try {
                        Thread.sleep(1000)
                    } catch (InterruptedException ie) {
                        break
                    }
                }
            }
        }
        connectorThread = new Thread(r)
        connectorThread.setDaemon(true)
        connectorThread.setName("I2CP Connector")
        connectorThread.start()
    }
    
    void shutdown() {
        connectorThread?.interrupt()
        socketManager.destroySocketManager()
    }

    Endpoint connect(Destination dest) {
        if (!connected)
            throw new I2PException("No I2CP connection")
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
