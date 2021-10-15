package com.muwire.core.connection

import net.i2p.client.streaming.I2PSocketManager
import net.i2p.data.Destination

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

class I2PConnector {

    private static final int PERMITS = 4
    
    final I2PSocketManager socketManager
    
    private final Map<Destination, Semaphore> limiter = new ConcurrentHashMap<>()

    I2PConnector() {}

    I2PConnector(I2PSocketManager socketManager) {
        this.socketManager = socketManager
    }

    Endpoint connect(Destination dest) {
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
