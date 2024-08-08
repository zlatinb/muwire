package com.muwire.core

import net.i2p.client.I2PSession
import net.i2p.client.streaming.I2PSocketManager

class RouterConnectedEvent extends Event {
    I2PSession session
    I2PSocketManager socketManager
}
