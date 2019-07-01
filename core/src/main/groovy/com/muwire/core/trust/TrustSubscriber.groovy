package com.muwire.core.trust

import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.connection.I2PConnector

import net.i2p.data.Destination

class TrustSubscriber {
    private final EventBus eventBus
    private final I2PConnector i2pConnector
    private final MuWireSettings settings
    
    private final Map<Destination, Long> lastRequestTime = new ConcurrentHashMap<>()
    
}
