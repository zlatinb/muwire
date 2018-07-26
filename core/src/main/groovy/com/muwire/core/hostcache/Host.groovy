package com.muwire.core.hostcache

import net.i2p.data.Destination

class Host {

	private static final int MAX_FAILURES = 3
	
	final Destination destination
	int failures,successes
	
	public Host(Destination destination) {
		this.destination = destination
	}

	synchronized void onConnect() {
		failures = 0
		successes++
	}
	
	synchronized void onFailure() {
		failures++
		successes = 0
	}
	
	synchronized boolean isFailed() {
		failures >= MAX_FAILURES
	}
	
	synchronized boolean hasSucceeded() {
		successes > 0
	}
}
