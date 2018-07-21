package com.muwire.core.connection

import java.io.InputStream
import java.io.OutputStream

import com.muwire.core.EventBus

import net.i2p.data.Destination

/**
 * Connection where the other side is a leaf. 
 * Such connections can only be incoming.
 * @author zab
 */
class LeafConnection extends Connection {

	public LeafConnection(EventBus eventBus, InputStream inputStream, OutputStream outputStream, Destination remoteSide) {
		super(eventBus, inputStream, outputStream, remoteSide, true);
	}

}
