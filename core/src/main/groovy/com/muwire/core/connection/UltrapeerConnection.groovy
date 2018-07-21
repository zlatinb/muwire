package com.muwire.core.connection

import java.io.InputStream
import java.io.OutputStream

import com.muwire.core.EventBus

import net.i2p.data.Destination

/**
 * Connection where this side is a leaf and the
 * other side an ultrapeer.  Such connections can only
 * be outgoing
 * @author zab
 */
class UltrapeerConnection extends Connection {

	public UltrapeerConnection(EventBus eventBus, InputStream inputStream, OutputStream outputStream,
			Destination remoteSide) {
		super(eventBus, inputStream, outputStream, remoteSide, false)
	}

}
