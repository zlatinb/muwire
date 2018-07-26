package com.muwire.core.connection

import java.io.InputStream
import java.io.OutputStream

import com.muwire.core.EventBus

import net.i2p.data.Destination

/**
 * This side is an ultrapeer and the remote is an ultrapeer too
 * @author zab
 */
class PeerConnection extends Connection {
	
	private final DataInputStream dis
	private final DataOutputStream dos

	public PeerConnection(EventBus eventBus, Endpoint endpoint,
			boolean incoming) {
		super(eventBus, endpoint, incoming)
		this.dis = new DataInputStream(endpoint.inputStream)
		this.dos = new DataOutputStream(endpoint.outputStream)
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
