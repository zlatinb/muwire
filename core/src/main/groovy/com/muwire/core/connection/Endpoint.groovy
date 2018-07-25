package com.muwire.core.connection

import java.util.concurrent.atomic.AtomicBoolean

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
class Endpoint implements Closeable {
	final Destination destination
	final InputStream inputStream
	final OutputStream outputStream
	
	private final AtomicBoolean closed = new AtomicBoolean()
	
	Endpoint(Destination destination, InputStream inputStream, OutputStream outputStream) {
		this.destination = destination
		this.inputStream = inputStream
		this.outputStream = outputStream
	}
	
	@Override
	public void close() {
		if (!closed.compareAndSet(false, true)) {
			log.warning("Close loop detected for ${destination.toBase32()}", new Exception())
			return
		}
		if (inputStream != null) {
			try {inputStream.close()} catch (Exception ignore) {}
		}
		if (outputStream != null) {
			try {outputStream.close()} catch (Exception ignore) {}
		}
	}
} 
