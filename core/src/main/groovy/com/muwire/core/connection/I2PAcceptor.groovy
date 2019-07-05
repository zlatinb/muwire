package com.muwire.core.connection

import net.i2p.client.streaming.I2PServerSocket
import net.i2p.client.streaming.I2PSocketManager

class I2PAcceptor {

    final I2PSocketManager socketManager
    final I2PServerSocket serverSocket

    I2PAcceptor() {}

    I2PAcceptor(I2PSocketManager socketManager) {
        this.socketManager = socketManager
        this.serverSocket = socketManager.getServerSocket()
    }

    Endpoint accept() {
        def socket = serverSocket.accept()
        new Endpoint(socket.getPeerDestination(), socket.getInputStream(), socket.getOutputStream(), socket)
    }
}
