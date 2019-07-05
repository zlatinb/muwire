package com.muwire.core.connection

import java.io.InputStream
import java.io.OutputStream

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.hostcache.HostCache
import com.muwire.core.trust.TrustService

import net.i2p.data.Destination

/**
 * Connection where the other side is a leaf.
 * Such connections can only be incoming.
 * @author zab
 */
class LeafConnection extends Connection {

    public LeafConnection(EventBus eventBus, Endpoint endpoint, HostCache hostCache,
        TrustService trustService, MuWireSettings settings) {
        super(eventBus, endpoint, true, hostCache, trustService, settings);
    }

    @Override
    protected void read() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void write(Object message) {
        // TODO Auto-generated method stub

    }

}
