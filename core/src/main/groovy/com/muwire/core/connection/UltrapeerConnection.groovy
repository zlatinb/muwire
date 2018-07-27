package com.muwire.core.connection

import java.io.InputStream
import java.io.OutputStream

import com.muwire.core.EventBus
import com.muwire.core.hostcache.HostCache

import net.i2p.data.Destination

/**
 * Connection where this side is a leaf and the
 * other side an ultrapeer.  Such connections can only
 * be outgoing
 * @author zab
 */
class UltrapeerConnection extends Connection {

	public UltrapeerConnection(EventBus eventBus, Endpoint endpoint, HostCache hostCache) {
		super(eventBus, endpoint, false, hostCache)
	}

	@Override
	protected void read() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void write(Object message) {
		if (message instanceof Map) {
			writeJsonMessage(message)
		} else {
			writeBinaryMessage(message)
		}
	}

	private void writeJsonMessage(def message) {
		
	}
	
	private void writeBinaryMessage(def message) {
		
	}
}
