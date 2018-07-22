package com.muwire.core.hostcache

import net.i2p.data.Destination

class Host {

	private static final int MAX_FAILURES = 3
	
	final Destination destination
	int failures
	
	public Host(Destination destination) {
		this.destination = destination
	}

	synchronized void onConnect() {
		failures = 0
	}
	
	synchronized void onFailure() {
		failures++
	}
	
	synchronized boolean isFailed() {
		failures >= MAX_FAILURES
	}
}
