package com.muwire.core.connection

import com.muwire.core.RouterConnectedEvent
import com.muwire.core.RouterDisconnectedEvent
import net.i2p.client.streaming.I2PServerSocket
import net.i2p.client.streaming.I2PSocketManager

import java.util.function.Supplier

class I2PAcceptor {

    private final Supplier<I2PSocketManager> socketManager
    private I2PServerSocket serverSocket

    I2PAcceptor() {}

    I2PAcceptor(Supplier<I2PSocketManager> socketManager) {
        this.socketManager = socketManager
    }
    
    synchronized void onRouterConnectedEvent(RouterConnectedEvent event) {
        serverSocket = socketManager.get().getServerSocket()
        notifyAll()
    }
    
    synchronized void onRouterDisconnectedEvent(RouterDisconnectedEvent event) {
        serverSocket = null
    }

    Endpoint accept() {
        I2PServerSocket serverSocket 
        synchronized (this) {
            while((serverSocket = this.serverSocket) == null)
                wait()
        }
        def socket = serverSocket.accept()
        new Endpoint(socket.getPeerDestination(), socket.getInputStream(), socket.getOutputStream(), socket)
    }
}
