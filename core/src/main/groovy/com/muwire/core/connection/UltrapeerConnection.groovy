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

	public UltrapeerConnection(EventBus eventBus, Endpoint endpoint) {
		super(eventBus, endpoint, false)
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
