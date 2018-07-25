package com.muwire.core.connection

import net.i2p.data.Destination

class Endpoint {
	final Destination destination
	final InputStream inputStream
	final OutputStream outputStream
	
	Endpoint(Destination destination, InputStream inputStream, OutputStream outputStream) {
		this.destination = destination
		this.inputStream = inputStream
		this.outputStream = outputStream
	}
} 
